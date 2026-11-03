package rajnishkmehta.sakshi.portal.ui.importing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaImportUiStateTest {

    @Test
    fun testIdleState() {
        val state = MediaImportUiState.Idle
        assertTrue(state is MediaImportUiState.Idle)
    }

    @Test
    fun testInProgressState() {
        val state = MediaImportUiState.InProgress(
            totalFiles = 10,
            completedFiles = 5,
            currentFileName = "test.jpg",
            failedFiles = 1
        )

        assertEquals(10, state.totalFiles)
        assertEquals(5, state.completedFiles)
        assertEquals("test.jpg", state.currentFileName)
        assertEquals(1, state.failedFiles)
    }

    @Test
    fun testCompletedState() {
        val state = MediaImportUiState.Completed(
            totalFiles = 10,
            successfulFiles = 9,
            failedFiles = 1
        )

        assertEquals(10, state.totalFiles)
        assertEquals(9, state.successfulFiles)
        assertEquals(1, state.failedFiles)
    }
}
