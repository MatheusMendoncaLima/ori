package com.oriteam.ori.domain.model

data class Vinculo(
    val id: String = "",
    val responsavelUid: String = "",
    val principalUid: String = "",
    val status: StatusVinculo = StatusVinculo.ATIVO,
    val criadoEm: Long = 0L,
    val encerradoEm: Long? = null
)
