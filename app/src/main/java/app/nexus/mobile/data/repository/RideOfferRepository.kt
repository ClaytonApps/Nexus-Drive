package app.nexus.mobile.data.repository

import app.nexus.mobile.data.model.RideOffer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class RideOfferRepository {

    private val _offers = MutableSharedFlow<RideOffer>(extraBufferCapacity = 16)
    val offers: SharedFlow<RideOffer> = _offers

    suspend fun publish(offer: RideOffer) {
        _offers.emit(offer)
    }
}
