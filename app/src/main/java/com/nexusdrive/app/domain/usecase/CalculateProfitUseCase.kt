package com.nexusdrive.app.domain.usecase

import com.nexusdrive.app.data.model.ProfitEstimate

/**
 * Calcula a rentabilidade bruta e líquida de uma oferta de corrida.
 *
 * O motorista configura previamente seu **custo por km** (soma de
 * combustível, manutenção, IPVA rateado, depreciação etc.). Aqui só
 * aplicamos o desconto desse custo sobre a receita oferecida pelo app.
 *
 * Para calcular o **lucro por hora** precisamos também de uma
 * estimativa de duração da corrida — aceita como parâmetro porque o
 * app de motorista normalmente exibe esse tempo na tela de oferta.
 */
class CalculateProfitUseCase {

    /**
     * @param rideValueBrl     Valor que o app pagará pela corrida (R$).
     * @param totalDistanceKm  Distância total: pickup + destino (km).
     * @param costPerKmBrl     Custo operacional configurado pelo motorista (R$/km).
     * @param estimatedDurationMin Duração total estimada (minutos).
     *        Inclui o deslocamento até o passageiro e a viagem em si.
     *        Necessário para extrapolar o lucro por hora.
     */
    operator fun invoke(
        rideValueBrl: Double,
        totalDistanceKm: Double,
        costPerKmBrl: Double,
        estimatedDurationMin: Int
    ): ProfitEstimate {
        // Garantia defensiva: nenhum input pode ser negativo.
        val revenue = rideValueBrl.coerceAtLeast(0.0)
        val distance = totalDistanceKm.coerceAtLeast(0.0)
        val costKm = costPerKmBrl.coerceAtLeast(0.0)
        val duration = estimatedDurationMin.coerceAtLeast(0)

        val totalCost = distance * costKm

        return ProfitEstimate(
            grossRevenueBrl = revenue,
            totalCostBrl = totalCost,
            totalDistanceKm = distance,
            totalDurationMin = duration
        )
    }
}
