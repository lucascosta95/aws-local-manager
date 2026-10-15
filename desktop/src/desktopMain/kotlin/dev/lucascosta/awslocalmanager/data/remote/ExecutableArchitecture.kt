package dev.lucascosta.awslocalmanager.data.remote

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Reads the Mach-O header to tell whether a binary has a slice for the CPU the app runs on.
object ExecutableArchitecture {
    private const val OS_ARCH = "os.arch"
    private const val HEADER_BYTES = 4096

    private const val MH_MAGIC = 0xFEEDFACE.toInt()
    private const val MH_MAGIC_64 = 0xFEEDFACF.toInt()
    private const val FAT_MAGIC = 0xCAFEBABE.toInt()
    private const val FAT_MAGIC_64 = 0xCAFEBABF.toInt()
    private const val FAT_ARCH_SIZE = 20
    private const val FAT_ARCH_64_SIZE = 32
    private const val MAX_FAT_ARCHS = 32

    private const val CPU_TYPE_X86_64 = 0x01000007
    private const val CPU_TYPE_ARM64 = 0x0100000C

    private val hostCpuType: Int? by lazy {
        when (System.getProperty(OS_ARCH)?.lowercase()) {
            "aarch64", "arm64" -> CPU_TYPE_ARM64
            "x86_64", "amd64" -> CPU_TYPE_X86_64
            else -> null
        }
    }

    // Anything that is not a Mach-O binary (scripts, ELF on Linux) or an unknown host is assumed to run.
    fun runsNatively(file: File): Boolean {
        val host = hostCpuType ?: return true
        val cpuTypes = runCatching { readCpuTypes(file) }.getOrNull() ?: return true
        return host in cpuTypes
    }

    private fun readCpuTypes(file: File): Set<Int>? {
        val header = ByteArray(HEADER_BYTES)
        val read = RandomAccessFile(file, "r").use { it.read(header) }
        if (read < Int.SIZE_BYTES * 2) return null
        val buffer = ByteBuffer.wrap(header, 0, read)

        val bigEndianMagic = buffer.order(ByteOrder.BIG_ENDIAN).getInt(0)
        if (bigEndianMagic == FAT_MAGIC || bigEndianMagic == FAT_MAGIC_64) {
            return readFatCpuTypes(buffer, if (bigEndianMagic == FAT_MAGIC) FAT_ARCH_SIZE else FAT_ARCH_64_SIZE)
        }

        val littleEndianMagic = buffer.order(ByteOrder.LITTLE_ENDIAN).getInt(0)
        if (littleEndianMagic == MH_MAGIC || littleEndianMagic == MH_MAGIC_64) {
            return setOf(buffer.getInt(Int.SIZE_BYTES))
        }
        return null
    }

    private fun readFatCpuTypes(
        buffer: ByteBuffer,
        archSize: Int,
    ): Set<Int>? {
        buffer.order(ByteOrder.BIG_ENDIAN)
        val count = buffer.getInt(Int.SIZE_BYTES)
        // Java class files share the 0xCAFEBABE magic; their version field reads as a large arch count.
        if (count !in 1..MAX_FAT_ARCHS) return null
        return (0 until count)
            .map { index -> Int.SIZE_BYTES * 2 + index * archSize }
            .takeWhile { offset -> offset + Int.SIZE_BYTES <= buffer.limit() }
            .map { offset -> buffer.getInt(offset) }
            .toSet()
    }
}
