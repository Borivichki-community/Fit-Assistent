package dev.andre.nutritionist.diary.grpc.validation

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DiaryRequestValidator {
    fun validate(request: AddEntryRequest) {
        validateId(request.entryId)
        if (request.hasSourceId() && request.sourceId.isNotBlank()) {
            validateId(request.sourceId)
        } else if (request.foodItemId.isNotBlank()) {
            validateId(request.foodItemId)
        }
        val name = if (request.nameSnapshot.isNotBlank()) request.nameSnapshot else request.foodName
        require(name.isNotBlank()) { "name_snapshot is required" }
        require(name.length <= 255) { "name_snapshot length must be <= 255" }
        val weight = if (request.weightGrams > 0.0) request.weightGrams else request.quantityGrams
        require(weight > 0.0 && weight.isFinite()) { "weight_grams must be positive" }

        require(request.hasNutrition()) { "nutrition is required" }
        require(request.hasRecordedAt()) { "recorded_at is required" }
        require(request.recordedAt.seconds in -62_135_596_800L..253_402_300_799L) { "Invalid recorded_at" }
        require(request.recordedAt.nanos in 0..999_999_999) { "Invalid timestamp nanos" }
        if (request.hasCost()) {
            require(request.cost.hasTotal()) { "cost.total is required" }
            require(request.cost.total.currencyCode == "RUB") { "Only RUB is supported" }
            if (request.cost.hasUnitPrice()) {
                require(request.cost.unitPrice.currencyCode == "RUB") { "Only RUB is supported" }
            }
            require(request.cost.hasUnitPrice() == request.cost.hasPriceBasisMilligrams()) { "Incomplete price basis" }
        }
    }

    fun validate(request: DeleteMealRequest) {
        validateId(request.entryId)
    }

    private fun validateId(value: String) {
        require(UUID.fromString(value).toString().equals(value, ignoreCase = true)) { "Invalid UUID" }
    }
}
