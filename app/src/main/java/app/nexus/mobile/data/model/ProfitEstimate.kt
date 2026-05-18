package app.nexus.mobile.data.model

/**
 * Resultado do cálculo de rentabilidade de uma oferta de corrida.
 *
 * Todos os valores monetários estão em BRL.
 * Distâncias em km, durações em minutos.
 */
data class ProfitEstimate(
    /** Valor que o app paga ao motorista (valor bruto da corrida). */
    val grossRevenueBrl: Double,
    /** Custo total estimado (combustível + manutenção + outros). */
    val totalCostBrl: Double,
    /** Distância total considerada: até o passageiro + até o destino. */
    val totalDistanceKm: Double,
    /** Duração total estimada da corrida (deslocamento + viagem), em minutos. */
    val totalDurationMin: Int
) {
    /** Lucro bruto: receita - 0 (sem descontar custos operacionais). */
    val grossProfitBrl: Double get() = grossRevenueBrl

    /** Lucro líquido: receita menos custos operacionais. */
    val netProfitBrl: Double get() = grossRevenueBrl - totalCostBrl

    /** Lucro líquido por km rodado. */
    val netProfitPerKm: Double
        get() = if (totalDistanceKm > 0) netProfitBrl / totalDistanceKm else 0.0

    /** Lucro líquido por hora (extrapolado a partir da duração estimada). */
    val netProfitPerHour: Double
        get() = if (totalDurationMin > 0) netProfitBrl * (60.0 / totalDurationMin) else 0.0

    /** Margem percentual sobre a receita bruta. */
    val marginPercent: Double
        get() = if (grossRevenueBrl > 0) (netProfitBrl / grossRevenueBrl) * 100.0 else 0.0
}
