package com.vikasr.todo.Controller;

import com.vikasr.todo.DTO.PagedTodoResponseDTO;
import com.vikasr.todo.DTO.TodoReorderRequestDTO;
import com.vikasr.todo.DTO.TodoRequestDTO;
import com.vikasr.todo.DTO.TodoAnalyticsResponseDTO;
import com.vikasr.todo.DTO.TodoResponseDTO;
import com.vikasr.todo.DTO.TodoListRequestDTO;
import com.vikasr.todo.DTO.TodoListResponseDTO;
import com.vikasr.todo.DTO.TodoListShareRequestDTO;
import com.vikasr.todo.DTO.TodoViewRequestDTO;
import com.vikasr.todo.DTO.TodoViewResponseDTO;
import com.vikasr.todo.Model.TodoPriority;
import com.vikasr.todo.Service.AnalyticsService;
import com.vikasr.todo.Service.ExcelService;
import com.vikasr.todo.Service.TodoService;
import com.vikasr.todo.Service.TodoListService;
import com.vikasr.todo.Service.TodoViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
@Tag(name = "Todos", description = "Todo management, timer, import/export, analytics, lists, sharing, and saved views")

public class TodoController {

    private TodoService todoService;
    private final ExcelService excelService;
    private final AnalyticsService analyticsService;
    private final TodoViewService todoViewService;
    private final TodoListService todoListService;

    @Autowired
    public TodoController(TodoService todoService,
                          ExcelService excelService,
                          AnalyticsService analyticsService,
                          TodoViewService todoViewService,
                          TodoListService todoListService) {
        this.todoService = todoService;
        this.excelService = excelService;
        this.analyticsService = analyticsService;
        this.todoViewService = todoViewService;
        this.todoListService = todoListService;
    }

    @PostMapping
    @Operation(summary = "Create a todo", description = "Creates a new todo item.")
    @ApiResponse(responseCode = "200", description = "Todo created",
            content = @Content(schema = @Schema(implementation = TodoResponseDTO.class)))
    public TodoResponseDTO createTodo(@Valid @RequestBody TodoRequestDTO request) {
        return todoService.createTodo(request);
    }

    @GetMapping
    @Operation(summary = "List todos", description = "Returns paged todos with optional search, sort, and filter parameters.")
    @ApiResponse(responseCode = "200", description = "Paged todo response",
            content = @Content(schema = @Schema(implementation = PagedTodoResponseDTO.class)))
    public PagedTodoResponseDTO getAllTodos(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "createdAt") String sortBy,
                                            @RequestParam(defaultValue = "desc") String direction,
                                            @RequestParam(required = false) String search,
                                            @RequestParam(required = false) Long listId,
                                            @RequestParam(required = false) String requesterEmail,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                                            @RequestParam(required = false) TodoPriority priority,
                                            @RequestParam(required = false) Boolean completed,
                                            @RequestParam(required = false) Boolean archived) {
        Sort sort = buildSort(sortBy, direction);
        Pageable pageable = PageRequest.of(page, size, sort);
        return todoService.getAllTodo(pageable, search, listId, requesterEmail, category, tag, fromDate, toDate, priority, completed, archived);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a todo", description = "Updates an existing todo by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todo updated",
                    content = @Content(schema = @Schema(implementation = TodoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found")
    })
    public TodoResponseDTO updateTodo(@PathVariable Long id,
                                      @RequestBody TodoRequestDTO request) {

        return todoService.updateTodo(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a todo", description = "Deletes a todo by id.")
    @ApiResponse(responseCode = "200", description = "Todo deleted")
    public void deleteTodo(@PathVariable Long id,
                           @RequestParam(required = false) String requesterEmail) {
        todoService.deleteTodo(id, requesterEmail);
    }

    @PostMapping("/reorder")
    @Operation(summary = "Reorder todos", description = "Updates todo display order based on the provided id sequence.")
    @ApiResponse(responseCode = "200", description = "Todos reordered")
    public void reorderTodos(@Valid @RequestBody TodoReorderRequestDTO request,
                             @RequestParam(required = false) String requesterEmail) {
        todoService.reorderTodos(request, requesterEmail);
    }

    @PostMapping("/undo")
    @Operation(summary = "Undo last action")
    public void undoLastAction() {
        todoService.undoLastAction();
    }

    @PostMapping("/redo")
    @Operation(summary = "Redo last undone action")
    public void redoLastAction() {
        todoService.redoLastAction();
    }

    @PostMapping("/{id}/timer/start")
    @Operation(summary = "Start todo timer")
    @ApiResponse(responseCode = "200", description = "Timer started",
            content = @Content(schema = @Schema(implementation = TodoResponseDTO.class)))
    public TodoResponseDTO startTimer(@PathVariable Long id,
                                      @RequestParam(required = false) String requesterEmail) {
        return todoService.startTimer(id, requesterEmail);
    }

    @PostMapping("/{id}/timer/pause")
    @Operation(summary = "Pause todo timer")
    @ApiResponse(responseCode = "200", description = "Timer paused",
            content = @Content(schema = @Schema(implementation = TodoResponseDTO.class)))
    public TodoResponseDTO pauseTimer(@PathVariable Long id,
                                      @RequestParam(required = false) String requesterEmail) {
        return todoService.pauseTimer(id, requesterEmail);
    }

    @PostMapping("/{id}/timer/stop")
    @Operation(summary = "Stop todo timer")
    @ApiResponse(responseCode = "200", description = "Timer stopped",
            content = @Content(schema = @Schema(implementation = TodoResponseDTO.class)))
    public TodoResponseDTO stopTimer(@PathVariable Long id,
                                     @RequestParam(required = false) String requesterEmail) {
        return todoService.stopTimer(id, requesterEmail);
    }

    @PostMapping("/{id}/send-reminder-email")
    @Operation(summary = "Send reminder email now")
    @ApiResponse(responseCode = "200", description = "Reminder email sent",
            content = @Content(schema = @Schema(implementation = TodoResponseDTO.class)))
    public TodoResponseDTO sendReminderEmail(@PathVariable Long id,
                                             @RequestParam(required = false) String requesterEmail) {
        return todoService.sendReminderEmail(id, requesterEmail);
    }

    // ✅ EXPORT
    @GetMapping("/export")
    @Operation(summary = "Export todos", description = "Exports todos to an Excel file.")
    @ApiResponse(responseCode = "200", description = "Excel file stream")
    public void exportExcel(HttpServletResponse response,
                            @RequestParam(required = false) Long listId,
                            @RequestParam(required = false) String requesterEmail) {
        List<TodoResponseDTO> todos = todoService.getAllTodo(
                        PageRequest.of(0, Integer.MAX_VALUE, Sort.by("createdAt").descending()),
                        null,
                        listId,
                        requesterEmail,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)
                .getContent();
        excelService.exportTodos(response, todos);
    }

    @PostMapping("/lists")
    @Operation(summary = "Create a list")
    @ApiResponse(responseCode = "200", description = "List created",
            content = @Content(schema = @Schema(implementation = TodoListResponseDTO.class)))
    public TodoListResponseDTO createList(@Valid @RequestBody TodoListRequestDTO request) {
        return todoListService.createList(request);
    }

    @GetMapping("/lists")
    @Operation(summary = "Get accessible lists")
    @ApiResponse(responseCode = "200", description = "Accessible lists",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TodoListResponseDTO.class))))
    public List<TodoListResponseDTO> getAccessibleLists(@RequestParam String email) {
        return todoListService.getAccessibleLists(email);
    }

    @PostMapping("/lists/{listId}/share")
    @Operation(summary = "Share a list")
    @ApiResponse(responseCode = "200", description = "List shared",
            content = @Content(schema = @Schema(implementation = TodoListResponseDTO.class)))
    public TodoListResponseDTO shareList(@PathVariable Long listId,
                                         @Valid @RequestBody TodoListShareRequestDTO request) {
        return todoListService.shareList(listId, request);
    }

    // ✅ IMPORT
    @PostMapping("/import")
    @Operation(summary = "Import todos", description = "Imports todos from an uploaded Excel file.")
    @ApiResponse(responseCode = "200", description = "Import completed")
    public String importExcel(@RequestParam("file") MultipartFile file) {
        excelService.importTodos(file);
        return "Imported Successfully!";
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get analytics")
    @ApiResponse(responseCode = "200", description = "Analytics response",
            content = @Content(schema = @Schema(implementation = TodoAnalyticsResponseDTO.class)))
    public TodoAnalyticsResponseDTO getTodoAnalytics() {
        return analyticsService.getTodoAnalytics();
    }

    @PostMapping("/views")
    @Operation(summary = "Create a saved view")
    @ApiResponse(responseCode = "200", description = "View created",
            content = @Content(schema = @Schema(implementation = TodoViewResponseDTO.class)))
    public TodoViewResponseDTO createView(@Valid @RequestBody TodoViewRequestDTO request) {
        return todoViewService.createView(request);
    }

    @GetMapping("/views")
    @Operation(summary = "Get saved views")
    @ApiResponse(responseCode = "200", description = "Saved views",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TodoViewResponseDTO.class))))
    public List<TodoViewResponseDTO> getAllViews() {
        return todoViewService.getAllViews();
    }

    @PutMapping("/views/{id}")
    @Operation(summary = "Update a saved view")
    @ApiResponse(responseCode = "200", description = "View updated",
            content = @Content(schema = @Schema(implementation = TodoViewResponseDTO.class)))
    public TodoViewResponseDTO updateView(@PathVariable Long id,
                                          @Valid @RequestBody TodoViewRequestDTO request) {
        return todoViewService.updateView(id, request);
    }

    @DeleteMapping("/views/{id}")
    @Operation(summary = "Delete a saved view")
    @ApiResponse(responseCode = "200", description = "View deleted")
    public void deleteView(@PathVariable Long id) {
        todoViewService.deleteView(id);
    }

    private Sort buildSort(String sortBy, String direction) {
        boolean ascending = direction.equalsIgnoreCase("asc");

        if ("priority".equalsIgnoreCase(sortBy)) {
            String priorityOrder = """
                    case priority
                        when 'HIGH' then 0
                        when 'MEDIUM' then 1
                        when 'LOW' then 2
                        else 3
                    end
                    """;
            return ascending ? JpaSort.unsafe(priorityOrder).ascending() : JpaSort.unsafe(priorityOrder).descending();
        }

        return ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    }
}
