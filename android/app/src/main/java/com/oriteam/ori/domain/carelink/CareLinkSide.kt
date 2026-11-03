package com.oriteam.ori.domain.carelink

/**
 * Which side of the care links a screen shows. There is one kind of account, and the same account can be
 * on both sides of different links:
 * - [PRIMARY_USER]: the person who is supported, who sees and manages their caregivers (Perfil);
 * - [CAREGIVER]: the person who supports others, who sees the people they follow (Acompanhados).
 *
 * The names match the fields of a link (`primaryUserUid`, `caregiverUid`), not a kind of account.
 */
enum class CareLinkSide {
    PRIMARY_USER,
    CAREGIVER
}
