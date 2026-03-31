package com.vikasr.todo.Service.impl;

import com.vikasr.todo.DTO.TodoRequestDTO;
import com.vikasr.todo.DTO.TodoResponseDTO;
import com.vikasr.todo.Model.RecurrencePattern;
import com.vikasr.todo.Model.TodoPriority;
import com.vikasr.todo.Service.ExcelService;
import com.vikasr.todo.Service.TodoService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelServiceImpl implements ExcelService {

    private final TodoService todoService;

    @Override
    public void exportTodos(HttpServletResponse response, List<TodoResponseDTO> todos) {

        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Todos");

            Row header = sheet.createRow(0);
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

            int rowNum = 1;

            for (TodoResponseDTO todo : todos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(todo.getId());
                row.createCell(1).setCellValue(todo.getTitle());
                row.createCell(2).setCellValue(todo.getDescription());
                row.createCell(3).setCellValue(todo.getEmail());
                row.createCell(4).setCellValue(todo.getDueTime() != null ? todo.getDueTime().toString() : "");
                row.createCell(5).setCellValue(todo.getReminderTime() != null ? todo.getReminderTime().toString() : "");
                row.createCell(6).setCellValue(todo.getReminderMinutesBefore() != null ? todo.getReminderMinutesBefore() : 0);
                row.createCell(7).setCellValue(todo.getCategory() != null ? todo.getCategory() : "");
                row.createCell(8).setCellValue(todo.getTags() != null ? String.join(",", todo.getTags()) : "");
                row.createCell(9).setCellValue(todo.getRecurrencePattern() != null ? todo.getRecurrencePattern().name() : RecurrencePattern.NONE.name());
                row.createCell(10).setCellValue(todo.getRecurrenceInterval() != null ? todo.getRecurrenceInterval() : 1);
                row.createCell(11).setCellValue(todo.getPriority() != null ? todo.getPriority().name() : TodoPriority.MEDIUM.name());
                row.createCell(12).setCellValue(todo.isCompleted());
                row.createCell(13).setCellValue(Boolean.TRUE.equals(todo.getArchived()));
            }

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=todos.xlsx");

            workbook.write(response.getOutputStream());
            workbook.close();

        } catch (Exception e) {
            throw new RuntimeException("Error exporting Excel", e);
        }
    }

    @Override
    public void importTodos(MultipartFile file) {

        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                TodoRequestDTO dto = new TodoRequestDTO();
                dto.setTitle(getStringCellValue(row.getCell(1)));
                dto.setDescription(getStringCellValue(row.getCell(2)));
                dto.setEmail(getStringCellValue(row.getCell(3)));

                boolean newFormat = row.getLastCellNum() >= 14;
                String primaryScheduleValue = getStringCellValue(row.getCell(4));
                String secondaryScheduleValue = getStringCellValue(row.getCell(5));
                if (newFormat) {
                    if (!primaryScheduleValue.isBlank()) {
                        dto.setDueTime(LocalDateTime.parse(primaryScheduleValue));
                    }
                    if (!secondaryScheduleValue.isBlank()) {
                        dto.setReminderTime(LocalDateTime.parse(secondaryScheduleValue));
                    }
                    Integer reminderMinutesBefore = getIntegerCellValue(row.getCell(6));
                    if (reminderMinutesBefore != null) {
                        dto.setReminderMinutesBefore(reminderMinutesBefore);
                    }
                } else if (!primaryScheduleValue.isBlank()) {
                    dto.setReminderTime(LocalDateTime.parse(primaryScheduleValue));
                }

                int categoryColumn = newFormat ? 7 : 5;
                int tagsColumn = newFormat ? 8 : 6;
                int recurrencePatternColumn = newFormat ? 9 : 7;
                int recurrenceIntervalColumn = newFormat ? 10 : 8;
                int priorityColumn = newFormat ? 11 : 9;
                int completedColumn = newFormat ? 12 : 10;
                int archivedColumn = newFormat ? 13 : 11;

                dto.setCategory(getStringCellValue(row.getCell(categoryColumn)));
                if (row.getCell(tagsColumn) != null) {
                    String tags = getStringCellValue(row.getCell(tagsColumn));
                    if (!tags.isBlank()) {
                        dto.setTags(Arrays.stream(tags.split(","))
                                .map(String::trim)
                                .filter(tag -> !tag.isBlank())
                                .collect(Collectors.toCollection(LinkedHashSet::new)));
                    }
                }
                String recurrencePattern = getStringCellValue(row.getCell(recurrencePatternColumn));
                if (!recurrencePattern.isBlank()) {
                    dto.setRecurrencePattern(RecurrencePattern.valueOf(recurrencePattern.toUpperCase()));
                }
                Integer recurrenceInterval = getIntegerCellValue(row.getCell(recurrenceIntervalColumn));
                if (recurrenceInterval != null) {
                    dto.setRecurrenceInterval(recurrenceInterval);
                }
                String priority = getStringCellValue(row.getCell(priorityColumn));
                if (!priority.isBlank()) {
                    dto.setPriority(TodoPriority.valueOf(priority.toUpperCase()));
                }
                Boolean completed = getBooleanCellValue(row.getCell(completedColumn));
                if (completed != null) {
                    dto.setCompleted(completed);
                }
                Boolean archived = getBooleanCellValue(row.getCell(archivedColumn));
                if (archived != null) {
                    dto.setArchived(archived);
                }

                todoService.createTodo(dto);
            }

            workbook.close();

        } catch (Exception e) {
            throw new RuntimeException("Error importing Excel", e);
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> Double.toString(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Integer getIntegerCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> cell.getStringCellValue().isBlank() ? null : Integer.parseInt(cell.getStringCellValue().trim());
            default -> null;
        };
    }

    private Boolean getBooleanCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> cell.getStringCellValue().isBlank() ? null : Boolean.parseBoolean(cell.getStringCellValue().trim());
            default -> null;
        };
    }
}
