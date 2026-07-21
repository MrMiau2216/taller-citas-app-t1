package edu.pe.cibertec.taller.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		String placa = "MEZ-150";

		Mecanico mecanico = new Mecanico(
				1L,
				"Josef Antoni Meza",
				TipoServicio.CAMBIO_ACEITE
		);

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 10, 0);

		LocalDateTime fechaActual =
				LocalDateTime.of(2026, 9, 9, 8, 0);

		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanico));

		when(proveedorFechaHora.ahora())
				.thenReturn(fechaActual);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				1L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita citaGuardada = servicioCitas.agendarCita(
				1L,
				placa,
				TipoServicio.CAMBIO_ACEITE,
				fechaCita
		);

		// Assert
		assertEquals(
				EstadoCita.PROGRAMADA,
				citaGuardada.getEstado()
		);

		assertEquals(
				1,
				citaGuardada.getDuracionHoras()
		);

		verify(repositorioCitas, times(1))
				.save(any(Cita.class));

		verify(servicioNotificaciones, times(1))
				.notificarCitaAgendada(citaGuardada);
	}
	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		// Arrange
		Long idMecanico = 99L;

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 10, 0);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.empty());

		// Act
		MecanicoNoEncontradoException excepcion = assertThrows(
				MecanicoNoEncontradoException.class,
				() -> servicioCitas.agendarCita(
						idMecanico,
						"MEZ-150",
						TipoServicio.CAMBIO_ACEITE,
						fechaCita
				)
		);

		// Assert
		assertTrue(excepcion.getMessage().contains("99"));

		verify(repositorioCitas, never())
				.save(any(Cita.class));
	}
	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		String placa = "MEZ-150";

		Mecanico mecanico = new Mecanico(
				1L,
				"Josef Antoni Meza",
				TipoServicio.CAMBIO_ACEITE
		);

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 10, 0);

		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanico));

		// Act
		EspecialidadIncorrectaException excepcion = assertThrows(
				EspecialidadIncorrectaException.class,
				() -> servicioCitas.agendarCita(
						1L,
						placa,
						TipoServicio.REPARACION_MOTOR,
						fechaCita
				)
		);

		// Assert
		assertTrue(
				excepcion.getMessage().contains("REPARACION_MOTOR")
		);

		verify(repositorioCitas, never())
				.save(any(Cita.class));
	}
	@Test
	@DisplayName("Una reparacion de motor a las 07:00 se rechaza")
	void agendarServicioPesadoALasSiete() {
		// Arrange
		String placa = "MEZ-150";

		Mecanico mecanico = new Mecanico(
				1L,
				"Josef Antoni Meza",
				TipoServicio.REPARACION_MOTOR
		);

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 7, 0);

		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanico));

		// Act
		HorarioNoPermitidoException excepcion = assertThrows(
				HorarioNoPermitidoException.class,
				() -> servicioCitas.agendarCita(
						1L,
						placa,
						TipoServicio.REPARACION_MOTOR,
						fechaCita
				)
		);

		// Assert
		assertTrue(excepcion.getMessage().contains("08:00"));

		verify(repositorioCitas, never())
				.save(any(Cita.class));
	}

	@Test
	@DisplayName("Una reparacion de motor a las 08:00 se acepta")
	void agendarServicioPesadoALasOcho() {
		// Arrange
		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 8, 0);

		Mecanico mecanico = new Mecanico(
				1L,
				"Josef Antoni Meza",
				TipoServicio.REPARACION_MOTOR
		);

		LocalDateTime fechaActual =
				LocalDateTime.of(2026, 9, 9, 8, 0);

		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanico));

		when(proveedorFechaHora.ahora())
				.thenReturn(fechaActual);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				1L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita citaGuardada = servicioCitas.agendarCita(
				1L,
				"MEZ-150",
				TipoServicio.REPARACION_MOTOR,
				fechaCita
		);

		// Assert
		assertEquals(
				EstadoCita.PROGRAMADA,
				citaGuardada.getEstado()
		);

		assertEquals(
				4,
				citaGuardada.getDuracionHoras()
		);

		verify(repositorioCitas, times(1))
				.save(any(Cita.class));
	}

	@Test
	@DisplayName("Una reparacion de motor a las 11:00 se acepta")
	void agendarServicioPesadoALasOnce() {
		// Arrange
		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 11, 0);

		Mecanico mecanico = new Mecanico(
				1L,
				"Josef Antoni Meza",
				TipoServicio.REPARACION_MOTOR
		);

		LocalDateTime fechaActual =
				LocalDateTime.of(2026, 9, 9, 8, 0);

		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanico));

		when(proveedorFechaHora.ahora())
				.thenReturn(fechaActual);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				1L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita citaGuardada = servicioCitas.agendarCita(
				1L,
				"MEZ-150",
				TipoServicio.REPARACION_MOTOR,
				fechaCita
		);

		// Assert
		assertEquals(
				EstadoCita.PROGRAMADA,
				citaGuardada.getEstado()
		);

		assertEquals(
				4,
				citaGuardada.getDuracionHoras()
		);

		verify(repositorioCitas, times(1))
				.save(any(Cita.class));
	}

	@Test
	@DisplayName("Una reparacion de motor a las 12:00 se rechaza")
	void agendarServicioPesadoALasDoce() {
		// Arrange
		String placa = "MEZ-150";

		Mecanico mecanico = new Mecanico(
				1L,
				"Josef Antoni Meza",
				TipoServicio.REPARACION_MOTOR
		);

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 12, 0);

		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanico));

		// Act
		HorarioNoPermitidoException excepcion = assertThrows(
				HorarioNoPermitidoException.class,
				() -> servicioCitas.agendarCita(
						1L,
						placa,
						TipoServicio.REPARACION_MOTOR,
						fechaCita
				)
		);

		// Assert
		assertTrue(excepcion.getMessage().contains("12:00"));

		verify(repositorioCitas, never())
				.save(any(Cita.class));
	}
}
