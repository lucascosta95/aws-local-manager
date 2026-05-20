package dev.lucascosta.awslocalmanager.components

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.util.concurrent.CountDownLatch
import javax.swing.JFileChooser

fun openDirectoryChooser(title: String? = null): String? {
    var result: String? = null
    val latch = CountDownLatch(1)

    EventQueue.invokeLater {
        val chooser =
            JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                if (title != null) dialogTitle = title
            }

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            result = chooser.selectedFile.absolutePath
        }

        latch.countDown()
    }

    latch.await()
    return result
}

fun openFileDialog(title: String? = null): String? {
    var result: String? = null
    val latch = CountDownLatch(1)

    EventQueue.invokeLater {
        val dialog = FileDialog(null as Frame?, title ?: EMPTY_STRING, FileDialog.LOAD)

        dialog.isVisible = true
        if (dialog.file != null && dialog.directory != null) {
            result = "${dialog.directory}${dialog.file}"
        }

        dialog.dispose()
        latch.countDown()
    }

    latch.await()
    return result
}
