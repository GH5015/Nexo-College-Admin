package com.example.collegeadmin.data.local

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        SubjectEntity::class, 
        AssignmentEntity::class, 
        SessionEntity::class, 
        TaskEntity::class, 
        NoteEntity::class,
        UserInfoEntity::class,
        AiStudySummaryEntity::class,
        GeneratedReviewEntity::class,
        AiStudyPlanEntity::class
    ],
    version = 21,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collegeDao(): CollegeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "college_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface CollegeDao {
    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjects(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: String)

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE subjectId = :subjectId")
    suspend fun deleteSessionsBySubject(subjectId: String)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)
    
    @Query("SELECT * FROM assignments")
    suspend fun getAllAssignments(): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE subjectId = :subjectId")
    suspend fun getAssignmentsForSubject(subjectId: String): List<AssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)
    
    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignment(id: String)

    @Query("SELECT * FROM user_info LIMIT 1")
    suspend fun getUserInfo(): UserInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserInfo(userInfo: UserInfoEntity)

    // AI Study Summaries
    @Query("SELECT * FROM ai_study_summaries WHERE examId = :examId")
    suspend fun getStudySummary(examId: String): AiStudySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySummary(summary: AiStudySummaryEntity)

    @Query("DELETE FROM ai_study_summaries WHERE examId = :examId")
    suspend fun deleteStudySummary(examId: String)

    // Generated Reviews
    @Query("SELECT * FROM generated_reviews WHERE id = :id")
    suspend fun getGeneratedReview(id: String): GeneratedReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedReview(review: GeneratedReviewEntity)

    @Query("DELETE FROM generated_reviews WHERE id = :id")
    suspend fun deleteGeneratedReview(id: String)

    // AI Study Plans
    @Query("SELECT * FROM ai_study_plans WHERE id = 'current_plan'")
    suspend fun getStudyPlan(): AiStudyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStudyPlan(plan: AiStudyPlanEntity)
}
