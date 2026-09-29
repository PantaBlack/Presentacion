// Utilidades compartidas por las paginas de horarios/clases (profesor, practicante, alumno):
// boton "Agregar a Google Calendar", el buscador de alumnos, y el toggle de "Editar".
document.addEventListener('DOMContentLoaded', function () {

    // ---- Buscador de alumnos (filtra la lista de checkboxes por nombre) ----
    const buscador = document.getElementById('buscarAlumno');
    if (buscador) {
        buscador.addEventListener('input', () => {
            const texto = buscador.value.trim().toLowerCase();
            document.querySelectorAll('#listaAlumnos .fila-alumno').forEach((fila) => {
                const nombre = fila.textContent.trim().toLowerCase();
                fila.style.display = nombre.includes(texto) ? '' : 'none';
            });
        });
    }

    // ---- Mostrar/ocultar un formulario oculto (editar, cancelar esta semana, etc.) ----
    document.querySelectorAll('.btn-editar-toggle, .btn-cancelar-toggle').forEach((btn) => {
        btn.addEventListener('click', () => {
            const destino = document.getElementById(btn.getAttribute('data-target'));
            if (destino) destino.classList.toggle('hidden');
        });
    });

    // ---- "Agregar a Google Calendar": la clase es semanal recurrente (solo dia +
    // hora, sin fecha fija), asi que se calcula la proxima fecha en que cae ese dia
    // y se arma el evento como recurrente cada semana. ----
    const DIAS_ISO = { LUNES: 1, MARTES: 2, MIERCOLES: 3, JUEVES: 4, VIERNES: 5, SABADO: 6, DOMINGO: 7 };

    function proximaFecha(diaIso) {
        const hoy = new Date();
        const diaHoyIso = hoy.getDay() === 0 ? 7 : hoy.getDay();
        let delta = diaIso - diaHoyIso;
        if (delta < 0) delta += 7;
        const fecha = new Date(hoy);
        fecha.setDate(hoy.getDate() + delta);
        return fecha;
    }

    function formatearFechaHora(fecha, horaStr) {
        const partes = horaStr.split(':');
        const h = String(partes[0]).padStart(2, '0');
        const m = String(partes[1] || '0').padStart(2, '0');
        const y = fecha.getFullYear();
        const mo = String(fecha.getMonth() + 1).padStart(2, '0');
        const d = String(fecha.getDate()).padStart(2, '0');
        return y + mo + d + 'T' + h + m + '00';
    }

    function agregarAGoogleCalendar(diaEnum, inicio, fin, materia, descripcion, ubicacion) {
        const diaIso = DIAS_ISO[diaEnum] || 1;
        const fecha = proximaFecha(diaIso);
        const params = new URLSearchParams({
            action: 'TEMPLATE',
            text: materia || '',
            dates: formatearFechaHora(fecha, inicio) + '/' + formatearFechaHora(fecha, fin),
            details: descripcion || '',
            location: ubicacion || '',
            recur: 'RRULE:FREQ=WEEKLY'
        });
        window.open('https://calendar.google.com/calendar/render?' + params.toString(), '_blank', 'noopener');
    }

    document.querySelectorAll('.btn-gcal').forEach((btn) => {
        btn.addEventListener('click', () => {
            agregarAGoogleCalendar(
                btn.getAttribute('data-dia'), btn.getAttribute('data-inicio'), btn.getAttribute('data-fin'),
                btn.getAttribute('data-materia'), btn.getAttribute('data-desc'), btn.getAttribute('data-loc')
            );
        });
    });
});
