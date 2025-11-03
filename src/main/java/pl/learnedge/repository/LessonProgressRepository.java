package pl.learnedge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.learnedge.model.Lesson;
import pl.learnedge.model.LessonProgress;
import pl.learnedge.model.User;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByLessonAndUser(Lesson lesson, User user);
    @Query("SELECT lp.lesson.id FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.lesson.course.id = :courseId AND lp.completed = true")
    List<Long> findCompletedLessonIdsByCourseIdAndUserId(@Param("courseId") Long courseId, @Param("userId") Long userId);
    long countByUserIdAndLessonInAndCompletedTrue(Long userId, List<Lesson> lessons);

}
