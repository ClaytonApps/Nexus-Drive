package app.nexus.mobile.data.source.remote

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoints do backend (Django REST). Trocar para o caminho real
 * quando o servidor estiver de pé. Para Firebase, basta substituir
 * a implementação por chamadas ao SDK do Firestore — a interface
 * pública dos repositórios continua igual.
 */
interface NexusDriveApi {

    @POST("v1/rides/accepted")
    suspend fun postAcceptedRide(
        @Body ride: AcceptedRideDto
    ): AcceptedRideResponse
}
