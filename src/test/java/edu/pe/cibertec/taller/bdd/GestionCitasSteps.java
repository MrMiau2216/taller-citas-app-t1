package edu.pe.cibertec.taller.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Mecanico mecanicoOcupado;
	private Cita citaResultado;
	private RuntimeException excepcionResultado;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);

		servicioCitas = new ServicioCitasImpl(
				repositorioMecanicos,
				repositorioCitas,
				proveedorFechaHora,
				servicioNotificaciones
		);

		citaResultado = null;
		excepcionResultado = null;
	}

	@Given("un mecanico tiene una cita programada de 10:00 a 12:00")
	public void unMecanicoTieneUnaCitaProgramada() {
		// Arrange
		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 10, 0);

		mecanicoOcupado = new Mecanico(
				1L,
				"Josef Antoni Meza",
				TipoServicio.MANTENIMIENTO_LIGERO
		);

		Cita citaProgramada = new Cita(
				1L,
				mecanicoOcupado,
				"MEZ-150",
				TipoServicio.MANTENIMIENTO_LIGERO,
				fechaCita,
				2,
				EstadoCita.PROGRAMADA
		);

		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanicoOcupado));

		when(proveedorFechaHora.ahora())
				.thenReturn(LocalDateTime.of(2026, 9, 9, 8, 0));

		when(repositorioCitas.findByMecanicoIdAndEstado(
				1L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of(citaProgramada));
	}

	@And("existe otro mecanico disponible para mantenimiento ligero")
	public void existeOtroMecanicoDisponible() {
		// Arrange
		Mecanico otroMecanico = new Mecanico(
				2L,
				"Josef Antoni Meza",
				TipoServicio.MANTENIMIENTO_LIGERO
		);

		when(repositorioMecanicos.findById(2L))
				.thenReturn(Optional.of(otroMecanico));

		when(repositorioCitas.findByMecanicoIdAndEstado(
				2L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));
	}

	@When("registro un mantenimiento ligero para la placa {string} con el otro mecanico a las 10:00")
	public void registroMantenimientoConOtroMecanico(String placa) {
		// Act
		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 10, 0);

		citaResultado = servicioCitas.agendarCita(
				2L,
				placa,
				TipoServicio.MANTENIMIENTO_LIGERO,
				fechaCita
		);
	}

	@Then("la nueva cita queda en estado PROGRAMADA")
	public void laNuevaCitaQuedaProgramada() {
		// Assert
		assertEquals(
				EstadoCita.PROGRAMADA,
				citaResultado.getEstado()
		);

		assertEquals(
				"MEZ-150",
				citaResultado.getPlacaVehiculo()
		);
	}

	@And("se guarda y se notifica el agendamiento una vez")
	public void seGuardaYSeNotificaElAgendamiento() {
		// Assert
		verify(repositorioCitas, times(1))
				.save(any(Cita.class));

		verify(servicioNotificaciones, times(1))
				.notificarCitaAgendada(citaResultado);
	}

	@When("intento registrar un mantenimiento ligero con el mecanico ocupado a las 11:00")
	public void intentoRegistrarConMecanicoOcupadoALasOnce() {
		// Act
		String placa = "MEZ-150";

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 11, 0);

		try {
			servicioCitas.agendarCita(
					1L,
					placa,
					TipoServicio.MANTENIMIENTO_LIGERO,
					fechaCita
			);
		} catch (HorarioOcupadoException excepcion) {
			excepcionResultado = excepcion;
		}
	}

	@Then("el registro se rechaza por horario ocupado")
	public void elRegistroSeRechazaPorHorarioOcupado() {
		// Assert
		assertTrue(
				excepcionResultado instanceof HorarioOcupadoException
		);

		verify(repositorioCitas, never())
				.save(any(Cita.class));

		verify(servicioNotificaciones, never())
				.notificarCitaAgendada(any(Cita.class));
	}

	@When("intento registrar un mantenimiento ligero con el mecanico ocupado a las 12:00")
	public void intentoRegistrarConMecanicoOcupadoALasDoce() {
		// Arrange
		String placa = "MEZ-150";

		LocalDateTime fechaCita =
				LocalDateTime.of(2026, 9, 10, 12, 0);

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		citaResultado = servicioCitas.agendarCita(
				1L,
				placa,
				TipoServicio.MANTENIMIENTO_LIGERO,
				fechaCita
		);
	}

	@Then("la cita se registra en estado PROGRAMADA")
	public void laCitaSeRegistraEnEstadoProgramada() {
		// Assert
		assertEquals(
				EstadoCita.PROGRAMADA,
				citaResultado.getEstado()
		);

		verify(repositorioCitas, times(1))
				.save(any(Cita.class));

		verify(servicioNotificaciones, times(1))
				.notificarCitaAgendada(citaResultado);
	}
}
