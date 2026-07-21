Feature: Gestion de citas del taller mecanico

  Scenario: Registrar un mantenimiento ligero con otro mecanico
    Given un mecanico tiene una cita programada de 10:00 a 12:00
    And existe otro mecanico disponible para mantenimiento ligero
    When registro un mantenimiento ligero para la placa "MEZ-150" con el otro mecanico a las 10:00
    Then la nueva cita queda en estado PROGRAMADA
    And se guarda y se notifica el agendamiento una vez

  Scenario: Rechazar una cita que se cruza con el horario ocupado
    Given un mecanico tiene una cita programada de 10:00 a 12:00
    When intento registrar un mantenimiento ligero con el mecanico ocupado a las 11:00
    Then el registro se rechaza por horario ocupado

  Scenario: Registrar una cita cuando termina la cita anterior
    Given un mecanico tiene una cita programada de 10:00 a 12:00
    When intento registrar un mantenimiento ligero con el mecanico ocupado a las 12:00
    Then la cita se registra en estado PROGRAMADA
