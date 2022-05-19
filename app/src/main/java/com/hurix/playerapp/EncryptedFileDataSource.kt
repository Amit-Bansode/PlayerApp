package com.hurix.playerapp

import android.net.Uri
import com.google.android.exoplayer2.upstream.BaseDataSource
import kotlin.jvm.JvmOverloads
import com.google.android.exoplayer2.upstream.TransferListener
import kotlin.Throws
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.FileDataSource.FileDataSourceException
import android.text.TextUtils
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

class EncryptedFileDataSource private constructor(key: ByteArray, transformation: String?) :
  BaseDataSource(false) {
  class EncryptedFileDataSourceException : IOException {
    constructor(cause: Throwable?) : super(cause) {}
    constructor(message: String?, cause: IOException?) : super(message, cause) {}
  }

  /**
   * [DataSource.Factory] for [EncryptedFileDataSource] instances.
   */
  class Factory @JvmOverloads constructor(
    key: ByteArray? = null,
    transformation: String? = null
  ) : DataSource.Factory {
    private var listener: TransferListener? = null
    private lateinit var key: ByteArray
    private var transformation: String? = null
    fun setKey(key: ByteArray?) {
      if (key == null) {
        return
      }
      this.key = key
    }

    fun setTransformation(transformation: String?) {
      var transformation = transformation
      if (transformation == null) {
        transformation = "AES/CBC/NoPadding"
      }
      this.transformation = transformation
    }

    /**
     * Sets a [TransferListener] for [EncryptedFileDataSource] instances created by this factory.
     *
     * @param listener The [TransferListener].
     * @return This factory.
     */
    fun setListener(listener: TransferListener?): Factory {
      this.listener = listener
      return this
    }

    override fun createDataSource(): EncryptedFileDataSource {
      val dataSource = EncryptedFileDataSource(key, transformation)
      if (listener != null) {
        dataSource.addTransferListener(listener!!)
      }
      return dataSource
    }

    init {
      setKey(key)
      setTransformation(transformation)
    }
  }

  private var file: RandomAccessFile? = null
  private var uri: Uri? = null
  private var bytesRemaining: Long = 0
  private var opened = false
  private var key: ByteArray? = null
  private var transformation: String? = null
  private var cipher: Cipher? = null
  private var tmpBuffer: ByteArray? = null
  fun setKey(key: ByteArray?) {
    this.key = key
  }

  fun setTransformation(transformation: String?) {
    this.transformation = transformation
  }

  @Throws(EncryptedFileDataSourceException::class)
  override fun open(dataSpec: DataSpec): Long {
    try {
      val key = key
      val transformation = transformation
      val uri = dataSpec.uri
      this.uri = uri
      transferInitializing(dataSpec)
      if (key != null && transformation != null) {
        val keySpec = SecretKeySpec(key, "AES")
        cipher = Cipher.getInstance(transformation)
        cipher?.init(Cipher.DECRYPT_MODE, keySpec)
      }
      file = openLocalFile(uri)
      file!!.seek(dataSpec.position)
      bytesRemaining =
        if (dataSpec.length == C.LENGTH_UNSET.toLong()) file!!.length() - dataSpec.position else dataSpec.length
      if (bytesRemaining < 0) {
        throw EOFException()
      }
    } catch (e: IOException) {
      throw EncryptedFileDataSourceException(e)
    } catch (e: NoSuchPaddingException) {
      throw EncryptedFileDataSourceException(e)
    } catch (e: NoSuchAlgorithmException) {
      throw EncryptedFileDataSourceException(e)
    } catch (e: InvalidKeyException) {
      throw EncryptedFileDataSourceException(e)
    }
    opened = true
    transferStarted(dataSpec)
    return bytesRemaining
  }

  @Throws(EncryptedFileDataSourceException::class)
  override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
    return if (readLength == 0) {
      0
    } else if (bytesRemaining == 0L) {
      C.RESULT_END_OF_INPUT
    } else {
      val bytesRead: Int
      val bytesStored: Int
      try {
        val length = Math.min(bytesRemaining, readLength.toLong()).toInt()
        val cipher = cipher
        if (cipher != null) {
          tmpBuffer = resize(tmpBuffer, length)
          bytesRead = Util.castNonNull(file).read(tmpBuffer, 0, length)
          bytesStored = cipher.doFinal(tmpBuffer, 0, length, buffer, offset)
        } else {
          bytesRead = Util.castNonNull(file).read(buffer, offset, length)
          bytesStored = bytesRead
        }
      } catch (e: IOException) {
        throw EncryptedFileDataSourceException(e)
      } catch (e: BadPaddingException) {
        throw EncryptedFileDataSourceException(e)
      } catch (e: IllegalBlockSizeException) {
        throw EncryptedFileDataSourceException(e)
      } catch (e: ShortBufferException) {
        throw EncryptedFileDataSourceException(e)
      }
      if (bytesRead > 0) {
        bytesRemaining -= bytesRead.toLong()
        bytesTransferred(bytesRead)
      }
      bytesStored
    }
  }

  override fun getUri(): Uri? {
    return uri
  }

  @Throws(EncryptedFileDataSourceException::class)
  override fun close() {
    uri = null
    try {
      if (file != null) {
        file!!.close()
      }
    } catch (e: IOException) {
      throw EncryptedFileDataSourceException(e)
    } finally {
      file = null
      key = null
      transformation = null
      cipher = null
      if (opened) {
        opened = false
        transferEnded()
      }
      val tmpBuffer = tmpBuffer
      this.tmpBuffer = null
      if (tmpBuffer != null) {
        var i = tmpBuffer.size - 1
        while (i >= 0) {
          tmpBuffer[i] = 0.toByte()
          i -= 2
        }
      }
    }
  }

  companion object {
    fun resize(b: ByteArray?, newLen: Int): ByteArray? {
      if (newLen < 0) return b
      return if (b == null || b.size < newLen) {
        ByteArray(newLen)
      } else b
    }

    @Throws(FileDataSourceException::class)
    private fun openLocalFile(uri: Uri): RandomAccessFile {
      return try {
        RandomAccessFile(Assertions.checkNotNull(uri.path), "r")
      } catch (e: FileNotFoundException) {
        if (!TextUtils.isEmpty(uri.query) || !TextUtils.isEmpty(uri.fragment)) {
          throw FileDataSourceException(
            String.format(
              "uri has query and/or fragment, which are not supported. Did you call Uri.parse()"
                + " on a string containing '?' or '#'? Use Uri.fromFile(new File(path)) to"
                + " avoid this. path=%s,query=%s,fragment=%s",
              uri.path, uri.query, uri.fragment
            ),
            e
          )
        }
        throw FileDataSourceException(e)
      }
    }
  }

  init {
    setKey(key)
    setTransformation(transformation)
  }
}
