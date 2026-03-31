package com.vikasr.todo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vikasr.todo.DTO.TodoListRequestDTO;
import com.vikasr.todo.DTO.TodoListShareRequestDTO;
import com.vikasr.todo.Model.Todo;
import com.vikasr.todo.Repository.TodoHistoryRepo;
import com.vikasr.todo.Repository.TodoListRepo;
import com.vikasr.todo.Repository.TodoRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "app.mail.enabled=true",
                "management.health.mail.enabled=false"
        }
)
class TodoControllerIntegrationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TodoRepo todoRepo;

    @Autowired
    private TodoHistoryRepo todoHistoryRepo;

    @Autowired
    private TodoListRepo todoListRepo;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;

    @WithMockUser(username = "test@example.com", authorities = {"ROLE_USER"})
    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        todoHistoryRepo.deleteAll();
        todoRepo.deleteAll();
        todoListRepo.deleteAll();
    }

    @Test
    @Transactional
    void manualReminderEndpointSendsEmailImmediately() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Todo todo = new Todo();
        todo.setTitle("Send update");
        todo.setDescription("Notify stakeholder");
        todo.setEmail("owner@example.com");
        todo.setCreatedAt(LocalDateTime.now());
        todo.setCompleted(false);
        todo.setArchived(false);
        todo.setToken("test-token-123");
        Todo savedTodo = todoRepo.save(todo);

        String response = mockMvc.perform(post("/api/v1/todos/{id}/send-reminder-email", savedTodo.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> sentTodo = objectMapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        assertEquals(savedTodo.getId().intValue(), ((Number) sentTodo.get("id")).intValue());
        assertEquals(Boolean.TRUE, sentTodo.get("reminderSent"));

        Todo updatedTodo = todoRepo.findById(savedTodo.getId()).orElseThrow();
        assertTrue(Boolean.TRUE.equals(updatedTodo.getReminderSent()));
        assertTrue(updatedTodo.getToken() != null && !updatedTodo.getToken().isBlank());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @Transactional
    void completeFromEmailMarksTodoCompleteWhenTokenMatches() throws Exception {
        Todo todo = new Todo();
        todo.setTitle("Finish taxes");
        todo.setDescription("Submit return");
        todo.setEmail("owner@example.com");
        todo.setCreatedAt(LocalDateTime.now());
        todo.setDueTime(LocalDateTime.of(2026, 4, 2, 18, 30));
        todo.setCompleted(false);
        todo.setArchived(false);
        todo.setToken("secure-token");
        Todo savedTodo = todoRepo.save(todo);

        String response = mockMvc.perform(get("/api/v1/todos/complete/{id}", savedTodo.getId())
                        .param("token", "secure-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(response.contains("Task Completed"));
        assertTrue(response.contains("Finish taxes"));

        Todo completedTodo = todoRepo.findById(savedTodo.getId()).orElseThrow();
        assertTrue(completedTodo.isCompleted());
    }

    @Test
    void completeFromEmailShowsInvalidPageWhenTokenDoesNotMatch() throws Exception {
        Todo todo = new Todo();
        todo.setTitle("Pay rent");
        todo.setDescription("Transfer funds");
        todo.setEmail("owner@example.com");
        todo.setCreatedAt(LocalDateTime.now());
        todo.setCompleted(false);
        todo.setArchived(false);
        todo.setToken("real-token");
        Todo savedTodo = todoRepo.save(todo);

        String response = mockMvc.perform(get("/api/v1/todos/complete/{id}", savedTodo.getId())
                        .param("token", "wrong-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(response.contains("Link Invalid"));
        assertTrue(response.contains("This completion link is invalid or has expired."));

        Todo unchangedTodo = todoRepo.findById(savedTodo.getId()).orElseThrow();
        assertFalse(unchangedTodo.isCompleted());
    }

    @Test
    void createShareAndFetchAccessibleListsWorksAcrossOwnerAndMember() throws Exception {
        TodoListRequestDTO createRequest = new TodoListRequestDTO();
        createRequest.setName("Work");
        createRequest.setOwnerEmail("Owner@Example.com");

        String createResponse = postJsonOk("/api/v1/todos/lists", createRequest);

        Map<String, Object> createdList = objectMapper.readValue(createResponse, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        assertEquals("Work", createdList.get("name"));
        assertEquals("owner@example.com", createdList.get("ownerEmail"));

        Long listId = ((Number) createdList.get("id")).longValue();

        TodoListShareRequestDTO shareRequest = new TodoListShareRequestDTO();
        shareRequest.setOwnerEmail("owner@example.com");
        shareRequest.setMemberEmail("Member@Example.com");

        String shareResponse = postJsonOk("/api/v1/todos/lists/" + listId + "/share", shareRequest);

        Map<String, Object> sharedList = objectMapper.readValue(shareResponse, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        List<String> sharedWithEmails = objectMapper.convertValue(sharedList.get("sharedWithEmails"), new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        assertEquals(List.of("member@example.com"), sharedWithEmails);

        String accessibleListsResponse = mockMvc.perform(get("/api/v1/todos/lists")
                        .param("email", "member@example.com"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> accessibleLists = objectMapper.readValue(accessibleListsResponse, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        assertEquals(1, accessibleLists.size());
        assertEquals("Work", accessibleLists.getFirst().get("name"));
        assertEquals("owner@example.com", accessibleLists.getFirst().get("ownerEmail"));
    }

    @Test
    void shareListRejectsNonOwnerRequests() throws Exception {
        TodoListRequestDTO createRequest = new TodoListRequestDTO();
        createRequest.setName("Private");
        createRequest.setOwnerEmail("owner@example.com");

        String createResponse = postJsonOk("/api/v1/todos/lists", createRequest);

        Long listId = objectMapper.readTree(createResponse).get("id").asLong();

        TodoListShareRequestDTO shareRequest = new TodoListShareRequestDTO();
        shareRequest.setOwnerEmail("intruder@example.com");
        shareRequest.setMemberEmail("member@example.com");

        mockMvc.perform(post("/api/v1/todos/lists/{listId}/share", listId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Todo list not found or owner access denied"));
    }

    @Test
    @Transactional
    void timerEndpointsStartPauseAndStopTrackingTime() throws Exception {
        Todo todo = new Todo();
        todo.setTitle("Deep work");
        todo.setDescription("Focus session");
        todo.setEmail("owner@example.com");
        todo.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        todo.setCompleted(false);
        todo.setArchived(false);
        todo.setTrackedSeconds(12L);
        todo.setTimerRunning(false);
        Todo savedTodo = todoRepo.save(todo);

        String startedResponse = mockMvc.perform(post("/api/v1/todos/{id}/timer/start", savedTodo.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> startedTodo = objectMapper.readValue(startedResponse, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        assertEquals(Boolean.TRUE, startedTodo.get("timerRunning"));

        Todo startedEntity = todoRepo.findById(savedTodo.getId()).orElseThrow();
        startedEntity.setTimerStartedAt(LocalDateTime.now().minusSeconds(3));
        todoRepo.save(startedEntity);

        String pausedResponse = mockMvc.perform(post("/api/v1/todos/{id}/timer/pause", savedTodo.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> pausedTodo = objectMapper.readValue(pausedResponse, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        assertEquals(Boolean.FALSE, pausedTodo.get("timerRunning"));

        Todo pausedEntity = todoRepo.findById(savedTodo.getId()).orElseThrow();
        assertFalse(Boolean.TRUE.equals(pausedEntity.getTimerRunning()));
        assertTrue(pausedEntity.getTrackedSeconds() >= 15L);

        startedEntity = todoRepo.findById(savedTodo.getId()).orElseThrow();
        startedEntity.setTimerRunning(true);
        startedEntity.setTimerStartedAt(LocalDateTime.now().minusSeconds(2));
        todoRepo.save(startedEntity);

        mockMvc.perform(post("/api/v1/todos/{id}/timer/stop", savedTodo.getId()))
                .andExpect(status().isOk());

        Todo stoppedEntity = todoRepo.findById(savedTodo.getId()).orElseThrow();
        assertFalse(Boolean.TRUE.equals(stoppedEntity.getTimerRunning()));
        assertTrue(stoppedEntity.getTrackedSeconds() >= pausedEntity.getTrackedSeconds());
    }

    @Test
    @Transactional
    void undoAndRedoRestoreCreateAndDeleteOperations() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Write summary",
                                  "description": "Draft release notes",
                                  "email": "owner@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long todoId = objectMapper.readTree(createResponse).get("id").asLong();
        assertTrue(todoRepo.findById(todoId).isPresent());

        mockMvc.perform(post("/api/v1/todos/undo"))
                .andExpect(status().isOk());

        assertFalse(todoRepo.findById(todoId).isPresent());

        mockMvc.perform(post("/api/v1/todos/redo"))
                .andExpect(status().isOk());

        assertEquals(1, todoRepo.count());
        Long restoredTodoId = todoRepo.findAll().getFirst().getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/todos/{id}", restoredTodoId))
                .andExpect(status().isOk());

        assertEquals(0, todoRepo.count());

        mockMvc.perform(post("/api/v1/todos/undo"))
                .andExpect(status().isOk());

        assertEquals(1, todoRepo.count());

        mockMvc.perform(post("/api/v1/todos/redo"))
                .andExpect(status().isOk());

        assertEquals(0, todoRepo.count());
    }

    @Test
    @Transactional
    void sharedListMemberCanCreateUpdateDeleteAndReorderTodos() throws Exception {
        Long listId = createSharedList("owner@example.com", "member@example.com");

        String createTodoResponse = mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Shared task",
                                  "description": "Work through shared list",
                                  "email": "owner@example.com",
                                  "listId": %d,
                                  "requesterEmail": "member@example.com"
                                }
                                """.formatted(listId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long todoId = objectMapper.readTree(createTodoResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/todos")
                        .param("listId", listId.toString())
                        .param("requesterEmail", "member@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Shared task")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/todos/{id}", todoId)
                        .param("requesterEmail", "member@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Shared task updated",
                                  "description": "Updated by member",
                                  "email": "owner@example.com",
                                  "listId": %d,
                                  "requesterEmail": "member@example.com"
                                }
                                """.formatted(listId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Shared task updated")));

        mockMvc.perform(post("/api/v1/todos/reorder")
                        .param("requesterEmail", "member@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "todoIds": [%d]
                                }
                                """.formatted(todoId)))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/todos/{id}", todoId)
                        .param("requesterEmail", "member@example.com"))
                .andExpect(status().isOk());

        assertEquals(0, todoRepo.count());
    }

    @Test
    @Transactional
    void sharedListRejectsOutsiderForCreateReadUpdateDeleteAndReorder() throws Exception {
        Long listId = createSharedList("owner@example.com", "member@example.com");

        mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Blocked task",
                                  "description": "Should fail",
                                  "email": "owner@example.com",
                                  "listId": %d,
                                  "requesterEmail": "outsider@example.com"
                                }
                                """.formatted(listId)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Todo list access denied"));

        String createTodoResponse = mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Owner task",
                                  "description": "Created by owner",
                                  "email": "owner@example.com",
                                  "listId": %d,
                                  "requesterEmail": "owner@example.com"
                                }
                                """.formatted(listId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long todoId = objectMapper.readTree(createTodoResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/todos")
                        .param("listId", listId.toString())
                        .param("requesterEmail", "outsider@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Todo list access denied"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/todos/{id}", todoId)
                        .param("requesterEmail", "outsider@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Illegal update",
                                  "description": "Should fail",
                                  "email": "owner@example.com",
                                  "listId": %d,
                                  "requesterEmail": "outsider@example.com"
                                }
                                """.formatted(listId)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Todo list access denied"));

        mockMvc.perform(post("/api/v1/todos/reorder")
                        .param("requesterEmail", "outsider@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "todoIds": [%d]
                                }
                                """.formatted(todoId)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Todo list access denied"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/todos/{id}", todoId)
                        .param("requesterEmail", "outsider@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Todo list access denied"));
    }

    @Test
    void savedViewLifecycleWorksThroughController() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/todos/views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "High Priority Work",
                                  "search": "report",
                                  "category": "work",
                                  "tag": "urgent",
                                  "priority": "HIGH",
                                  "completed": false,
                                  "archived": false,
                                  "sortBy": "priority",
                                  "direction": "asc",
                                  "pageSize": 25
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long viewId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/todos/views"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("High Priority Work")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/todos/views/{id}", viewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated View",
                                  "search": "notes",
                                  "category": "planning",
                                  "tag": "backlog",
                                  "priority": "MEDIUM",
                                  "completed": true,
                                  "archived": false,
                                  "sortBy": "createdAt",
                                  "direction": "desc",
                                  "pageSize": 15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Updated View")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"pageSize\":15")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/todos/views/{id}", viewId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/todos/views"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    @Transactional
    void importAndExportExcelRoundTripWorks() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "todos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes());

        mockMvc.perform(multipart("/api/v1/todos/import").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Imported Successfully!"));

        assertEquals(1, todoRepo.count());
        Todo importedTodo = todoRepo.findAll().getFirst();
        assertEquals("Imported task", importedTodo.getTitle());
        assertEquals("Review spreadsheet", importedTodo.getDescription());
        assertEquals("import@example.com", importedTodo.getEmail());
        assertEquals("finance", importedTodo.getCategory());
        assertEquals(2, importedTodo.getTags().size());

        MvcResult exportResult = mockMvc.perform(get("/api/v1/todos/export"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] exportedBytes = exportResult.getResponse().getContentAsByteArray();
        assertTrue(exportedBytes.length > 0);
        assertEquals("attachment; filename=todos.xlsx", exportResult.getResponse().getHeader("Content-Disposition"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportedBytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("Title", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Imported task", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Review spreadsheet", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("import@example.com", sheet.getRow(1).getCell(3).getStringCellValue());
        }
    }

    private Long createSharedList(String ownerEmail, String memberEmail) throws Exception {
        TodoListRequestDTO createRequest = new TodoListRequestDTO();
        createRequest.setName("Shared");
        createRequest.setOwnerEmail(ownerEmail);

        String createListResponse = postJsonOk("/api/v1/todos/lists", createRequest);

        Long listId = objectMapper.readTree(createListResponse).get("id").asLong();

        TodoListShareRequestDTO shareRequest = new TodoListShareRequestDTO();
        shareRequest.setOwnerEmail(ownerEmail);
        shareRequest.setMemberEmail(memberEmail);

        postJsonOk("/api/v1/todos/lists/" + listId + "/share", shareRequest);

        return listId;
    }

    private String postJsonOk(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private byte[] createWorkbookBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Todos");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Description");
            header.createCell(3).setCellValue("Email");
            header.createCell(4).setCellValue("Due Time");
            header.createCell(5).setCellValue("Reminder Time");
            header.createCell(6).setCellValue("Reminder Minutes Before");
            header.createCell(7).setCellValue("Category");
            header.createCell(8).setCellValue("Tags");
            header.createCell(9).setCellValue("Recurrence Pattern");
            header.createCell(10).setCellValue("Recurrence Interval");
            header.createCell(11).setCellValue("Priority");
            header.createCell(12).setCellValue("Completed");
            header.createCell(13).setCellValue("Archived");

            var row = sheet.createRow(1);
            row.createCell(1).setCellValue("Imported task");
            row.createCell(2).setCellValue("Review spreadsheet");
            row.createCell(3).setCellValue("import@example.com");
            row.createCell(4).setCellValue("2026-04-05T12:00:00");
            row.createCell(5).setCellValue("2026-04-05T11:45:00");
            row.createCell(6).setCellValue(15);
            row.createCell(7).setCellValue("finance");
            row.createCell(8).setCellValue("ops,report");
            row.createCell(9).setCellValue("NONE");
            row.createCell(10).setCellValue(1);
            row.createCell(11).setCellValue("HIGH");
            row.createCell(12).setCellValue(false);
            row.createCell(13).setCellValue(false);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
