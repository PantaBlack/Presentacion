package com.imfundokahle.service;

import com.imfundokahle.model.Material;
import com.imfundokahle.model.MaterialVisibility;
import com.imfundokahle.model.Role;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de materiales educativos.
 */
@Service
public class MaterialService {

    // Deben coincidir con el "length" de cada columna (ver Material): recortar aqui
    // evita que un titulo/descripcion muy largo tumbe el guardado con un error de
    // base de datos, sin importar desde que endpoint haya llegado.
    private static final int MAX_TITLE = 255;
    private static final int MAX_DESCRIPTION = 1000;

    private final MaterialRepository materialRepository;
    private final ScheduleService scheduleService;

    public MaterialService(MaterialRepository materialRepository, ScheduleService scheduleService) {
        this.materialRepository = materialRepository;
        this.scheduleService = scheduleService;
    }

    @Transactional
    public Material save(Material material) {
        material.setTitle(recortar(material.getTitle(), MAX_TITLE));
        material.setDescription(recortar(material.getDescription(), MAX_DESCRIPTION));
        return materialRepository.save(material);
    }

    private static String recortar(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }

    public Material findById(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    public List<Material> findAll() {
        return materialRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Material> findByTeacher(User teacher) {
        return materialRepository.findByTeacherOrderByCreatedAtDesc(teacher);
    }

    /**
     * Materiales visibles para un alumno: los generales (sin clase asignada, salvo lo
     * marcado "solo para profesores"), mas los exclusivos de alguna clase en la que
     * este activamente inscrito.
     */
    public List<Material> findVisibleToStudent(User student) {
        return materialRepository.findVisibleToStudent(student);
    }

    public long countByTeacher(User teacher) {
        return materialRepository.countByTeacher(teacher);
    }

    public long countAll() {
        return materialRepository.count();
    }

    /** Idempotente: si el material ya no existe (doble clic, dos pestañas abiertas) no falla, simplemente no hace nada. */
    @Transactional
    public void delete(Long id) {
        if (materialRepository.existsById(id)) {
            materialRepository.deleteById(id);
        }
    }

    /**
     * Si el usuario puede ver (y por lo tanto descargar) este material: el dueño y
     * el admin siempre pueden. Si el material tiene una clase asignada, ademas de eso
     * solo lo ven los alumnos activamente inscritos en esa clase (sin importar
     * "visibility"). Si no tiene clase, se rige por la visibilidad general de siempre.
     */
    public boolean esVisiblePara(Material material, User usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            return true;
        }
        if (material.getTeacher().getId().equals(usuario.getId())) {
            return true;
        }
        if (material.getSchedule() != null) {
            return usuario.getRole() == Role.STUDENT
                    && scheduleService.isStudentEnrolled(usuario, material.getSchedule());
        }
        if (material.getVisibility() == MaterialVisibility.TEACHERS_ONLY) {
            return usuario.getRole() == Role.TEACHER;
        }
        return true;
    }
}
