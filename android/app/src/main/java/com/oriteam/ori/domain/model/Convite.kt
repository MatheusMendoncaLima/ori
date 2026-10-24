package com.oriteam.ori.domain.model

data class Convite(
    val codigo: String = "",
    val responsavelUid: String = "",
    val expiraEm: Long = 0L,
    val status: StatusConvite = StatusConvite.PENDENTE
)
