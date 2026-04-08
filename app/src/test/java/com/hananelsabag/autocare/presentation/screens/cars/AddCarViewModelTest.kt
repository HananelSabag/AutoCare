package com.hananelsabag.autocare.presentation.screens.cars

import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddCarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: CarRepository
    private lateinit var viewModel: AddCarViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = AddCarViewModel(repository)
    }

    // ── Validation ──────────────────────────────────────────────────────────

    @Test
    fun `validate_emptyMake_setsError`() = runTest {
        viewModel.make = ""
        viewModel.model = "Corolla"
        viewModel.year = "2020"
        viewModel.licensePlate = "12-345-67"

        viewModel.save {}
        advanceUntilIdle()

        assertNotNull(viewModel.makeError)
        assertEquals(FieldError.Required, viewModel.makeError)
    }

    @Test
    fun `validate_emptyModel_setsError`() = runTest {
        viewModel.make = "Toyota"
        viewModel.model = ""
        viewModel.year = "2020"
        viewModel.licensePlate = "12-345-67"

        viewModel.save {}
        advanceUntilIdle()

        assertNotNull(viewModel.modelError)
        assertEquals(FieldError.Required, viewModel.modelError)
    }

    @Test
    fun `validate_invalidYear_tooLow_setsError`() = runTest {
        viewModel.make = "Toyota"
        viewModel.model = "Corolla"
        viewModel.year = "1800"
        viewModel.licensePlate = "12-345-67"

        viewModel.save {}
        advanceUntilIdle()

        assertEquals(FieldError.InvalidYear, viewModel.yearError)
    }

    @Test
    fun `validate_invalidYear_tooHigh_setsError`() = runTest {
        viewModel.make = "Toyota"
        viewModel.model = "Corolla"
        viewModel.year = "2031"
        viewModel.licensePlate = "12-345-67"

        viewModel.save {}
        advanceUntilIdle()

        assertEquals(FieldError.InvalidYear, viewModel.yearError)
    }

    @Test
    fun `validate_allValid_returnsTrue`() = runTest {
        coEvery { repository.insertCar(any()) } returns 1L

        viewModel.make = "Toyota"
        viewModel.model = "Corolla"
        viewModel.year = "2020"
        viewModel.licensePlate = "12-345-67"

        var onSavedCalled = false
        viewModel.save { onSavedCalled = true }
        advanceUntilIdle()

        // No errors
        assertNull(viewModel.makeError)
        assertNull(viewModel.modelError)
        assertNull(viewModel.yearError)
        assertNull(viewModel.licensePlateError)
        // onSaved was called
        assertTrue(onSavedCalled)
    }

    // ── Save ────────────────────────────────────────────────────────────────

    @Test
    fun `save_callsRepository_withCorrectData`() = runTest {
        coEvery { repository.insertCar(any()) } returns 42L

        viewModel.make = "Toyota"
        viewModel.model = "Corolla"
        viewModel.year = "2020"
        viewModel.licensePlate = "12-345-67"
        viewModel.color = "לבן"
        viewModel.currentKm = "85000"

        viewModel.save {}
        advanceUntilIdle()

        coVerify {
            repository.insertCar(match { car ->
                car.make == "Toyota" &&
                car.model == "Corolla" &&
                car.year == 2020 &&
                car.licensePlate == "12-345-67" &&
                car.color == "לבן" &&
                car.currentKm == 85000
            })
        }
    }

    @Test
    fun `save_editing_preservesCarId`() = runTest {
        val existingCar = Car(
            id = 5,
            make = "Old Make",
            model = "Old Model",
            year = 2018,
            licensePlate = "OLD-123",
            createdAt = 1_000_000L
        )
        every { repository.getCarById(5) } returns flowOf(existingCar)

        viewModel.loadCarForEdit(5)
        advanceUntilIdle()

        viewModel.make = "New Make"
        viewModel.model = "New Model"
        viewModel.year = "2022"
        viewModel.licensePlate = "NEW-456"

        viewModel.save {}
        advanceUntilIdle()

        coVerify {
            repository.updateCar(match { car ->
                car.id == 5 && car.createdAt == 1_000_000L
            })
        }
    }

    @Test
    fun `save_editing_preservesCreatedAt`() = runTest {
        val originalCreatedAt = 9_999_999L
        val existingCar = Car(
            id = 7,
            make = "Honda",
            model = "Civic",
            year = 2019,
            licensePlate = "AB-123-CD",
            createdAt = originalCreatedAt
        )
        every { repository.getCarById(7) } returns flowOf(existingCar)

        viewModel.loadCarForEdit(7)
        advanceUntilIdle()

        viewModel.notes = "updated notes"
        viewModel.save {}
        advanceUntilIdle()

        coVerify {
            repository.updateCar(match { car ->
                car.createdAt == originalCreatedAt
            })
        }
    }

    // ── Reset ───────────────────────────────────────────────────────────────

    @Test
    fun `resetForm_clearsAllFields`() = runTest {
        val existingCar = Car(
            id = 3, make = "Ford", model = "Focus", year = 2021,
            licensePlate = "XX-111-YY", color = "אדום", currentKm = 50000,
            notes = "some notes", createdAt = 500L
        )
        every { repository.getCarById(3) } returns flowOf(existingCar)

        viewModel.loadCarForEdit(3)
        advanceUntilIdle()

        // Sanity: form is populated
        assertEquals("Ford", viewModel.make)
        assertTrue(viewModel.isEditing)

        viewModel.resetForm()

        assertEquals("", viewModel.make)
        assertEquals("", viewModel.model)
        assertEquals("", viewModel.year)
        assertEquals("", viewModel.licensePlate)
        assertEquals("", viewModel.color)
        assertNull(viewModel.photoUri)
        assertEquals("", viewModel.currentKm)
        assertNull(viewModel.testExpiryDate)
        assertNull(viewModel.insuranceExpiryDate)
        assertEquals("", viewModel.notes)
        assertFalse(viewModel.isEditing)
        assertNull(viewModel.makeError)
        assertNull(viewModel.modelError)
        assertNull(viewModel.yearError)
        assertNull(viewModel.licensePlateError)
    }

    @Test
    fun `lastSavedCarId_isNull_afterClearLastSavedCarId`() = runTest {
        coEvery { repository.insertCar(any()) } returns 42L

        viewModel.make = "Toyota"
        viewModel.model = "Corolla"
        viewModel.year = "2020"
        viewModel.licensePlate = "12-345-67"

        viewModel.save {}
        advanceUntilIdle()

        // lastSavedCarId should be set after a successful new-car save
        assertNotNull(viewModel.lastSavedCarId.value)
        assertEquals(42, viewModel.lastSavedCarId.value)

        viewModel.clearLastSavedCarId()

        assertNull(viewModel.lastSavedCarId.value)
    }
}
