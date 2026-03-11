package com.example.collegeadmin.ai

import android.graphics.Bitmap
import com.example.collegeadmin.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AiAssistant(apiKey: String = BuildConfig.GEMINI_API_KEY) {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview", // Versão estável com suporte multimodal
        apiKey = apiKey
    )

    suspend fun askTutor(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "Não consegui gerar uma resposta."
        } catch (e: Exception) {
            "Erro na IA: ${e.localizedMessage}"
        }
    }

    fun askTutorStream(prompt: String): Flow<String> {
        return generativeModel.generateContentStream(prompt).map { it.text ?: "" }
    }

    suspend fun transcribeAudio(audioBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val prompt = "Transcreva este áudio de aula para texto de forma clara e organizada. Remova vícios de linguagem e foque no conteúdo técnico abordado."
        
        try {
            val inputContent = content {
                blob("audio/wav", audioBytes)
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            response.text ?: ""
        } catch (e: Exception) {
            "Erro na transcrição: ${e.localizedMessage}"
        }
    }

    suspend fun extractHistoryFromImage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val prompt = """
            Analise esta imagem de um histórico escolar ou boletim universitário.
            Extraia todas as disciplinas concluídas e suas respectivas notas e faltas.
            
            Retorne APENAS um JSON no seguinte formato (uma lista de objetos), sem blocos de código markdown ou texto adicional:
            [
              {
                "name": "Nome da Matéria",
                "professor": "Nome do Professor (se houver, senão use '')",
                "p1": 8.5,
                "p2": 7.0,
                "pf": null,
                "absences": 4
              }
            ]
            
            REQUISITOS:
            1. Se houver apenas uma nota final, coloque-a em 'p1' e deixe 'p2' e 'pf' como null.
            2. Se a nota for uma letra (A, B, C), converta para o equivalente numérico (10, 8, 6).
            3. Ignore matérias que não foram concluídas ou estão em curso.
        """.trimIndent()

        try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    suspend fun generateMindMap(notesContent: String): String {
        val prompt = """
            Com base nos seguintes conteúdos de estudo da semana, crie um MAPA MENTAL ESTRUTURADO em formato de texto.
            
            REQUISITOS DE FORMATAÇÃO:
            1. Use 🧠 para o TEMA CENTRAL no topo.
            2. Use 📁 para GRANDES ÁREAS/DISCIPLINAS.
            3. Use 🌿 para TÓPICOS PRINCIPAIS.
            4. Use 🔹 para DETALHES e CONCEITOS CHAVE.
            5. Use 💡 para INSIGHTS ou DICAS DE MEMORIZAÇÃO.
            6. Organize com recuos (identação) para mostrar a hierarquia claramente.
            
            O objetivo é transformar anotações desorganizadas em uma estrutura lógica que conecte os assuntos da semana.
            
            CONTEÚDO PARA PROCESSAR:
            $notesContent
        """.trimIndent()
        return askTutor(prompt)
    }

    suspend fun suggestStudyPlan(examsInfo: String, subjectsContent: String): String {
        val prompt = """
            Como um mentor de estudos especializado, crie um plano de estudos personalizado.
            
            DADOS DAS PROVAS:
            $examsInfo
            
            CONTEÚDO DAS DISCIPLINAS (Baseado nas aulas assistidas):
            $subjectsContent
            
            REQUISITOS DO PLANO:
            1. Organize por dias, começando de hoje até a data da última prova.
            2. Sugira o que estudar em cada bloco (ex: "Revisar Tópico X de POO").
            3. Priorize matérias com provas mais próximas ou com mayor volume de conteúdo.
            4. Use um tom motivador e organize em formato Markdown (com negritos e listas).
        """.trimIndent()
        return askTutor(prompt)
    }

    fun explainSpecificPointStream(content: String, point: String): Flow<String> {
        val prompt = """
            Com base no conteúdo de estudo abaixo, explique de forma mais detalhada e didática o seguinte ponto específico.
            
            CONTEÚDO ORIGINAL:
            $content
            
            PONTO PARA EXPLICAR MELHOR:
            $point
            
            Forneça uma explicação clara, exemplos se necessário, e organize com tópicos.
        """.trimIndent()
        return askTutorStream(prompt)
    }

    fun generateSubjectReviewStream(subjectName: String, notesContent: String): Flow<String> {
        val prompt = """
            Crie um material de revisão super didático e resumido para a matéria de $subjectName.
            Use como base as seguintes anotações de aula:
            
            $notesContent
            
            O material deve conter:
            1. Tópicos principais em negrito.
            2. Explicações curtas e diretas.
            3. Uma seção de "Dica de Ouro" para memorização.
            4. Formatação Markdown organizada.
        """.trimIndent()
        return askTutorStream(prompt)
    }

    suspend fun generateQuiz(subjectName: String, notesContent: String): String {
        val prompt = """
            Crie um quiz de 10 questões objetivas de múltipla escolha sobre a matéria de $subjectName baseando-se no conteúdo: $notesContent.
            Retorne APENAS um JSON no seguinte formato, sem blocos de código ou markdown:
            [
              {
                "question": "Pergunta 1?",
                "options": ["A", "B", "C", "D"],
                "correctIndex": 0
              },
              ...
            ]
        """.trimIndent()
        return askTutor(prompt)
    }
}
