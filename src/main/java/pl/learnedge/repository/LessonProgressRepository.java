package pl.learnedge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.learnedge.model.Lesson;
import pl.learnedge.model.LessonProgress;
import pl.learnedge.model.User;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByLessonAndUser(Lesson lesson, User user);
}
