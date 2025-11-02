package pl.learnedge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.learnedge.model.Lesson;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findAllByCourseId(Long courseId);
    Optional<Lesson> findBySlug(String slug);
    Optional<Lesson> findTopByCourseIdOrderByLessonOrderDesc(Long courseId);

}
