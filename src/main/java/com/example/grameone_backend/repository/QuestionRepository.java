package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByConceptId(Long conceptId);

    @Query("SELECT q FROM Question q " +
           "JOIN q.concept c " +
           "JOIN c.unit u " +
           "JOIN u.topic t " +
           "JOIN t.subject s " +
           "WHERE s.grade.id = :gradeId AND q.status = :status")
    List<Question> findByGradeIdAndStatus(@Param("gradeId") Long gradeId, @Param("status") String status);

    /** Count questions belonging to a specific grade (via concept -> unit -> topic -> subject -> grade) */
    @Query("SELECT COUNT(q) FROM Question q " +
           "JOIN q.concept c " +
           "JOIN c.unit u " +
           "JOIN u.topic t " +
           "JOIN t.subject s " +
           "WHERE s.grade.id = :gradeId")
    long countByGradeId(@Param("gradeId") Long gradeId);
}
