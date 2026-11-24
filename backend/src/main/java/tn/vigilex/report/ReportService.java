/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.common.ApiException;
import tn.vigilex.domain.*;
import tn.vigilex.repository.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {
    private final TenantTextRepository tenantTexts;
    private final LawTextRepository lawTexts;
    private final ActionRepository actions;
    private final EvidenceRepository evidence;
    private final TextDepartmentRepository textDepartments;
    private final DepartmentRepository departments;

    public ReportService(TenantTextRepository tenantTexts, LawTextRepository lawTexts, ActionRepository actions,
                         EvidenceRepository evidence, TextDepartmentRepository textDepartments,
                         DepartmentRepository departments) {
        this.tenantTexts = tenantTexts;
        this.lawTexts = lawTexts;
        this.actions = actions;
        this.evidence = evidence;
        this.textDepartments = textDepartments;
        this.departments = departments;
    }

    @Transactional(readOnly = true)
    public List<RegisterRow> register(UUID tenantId, String standardCode, UUID departmentId) {
        if (departmentId != null && departments.findByIdAndTenantId(departmentId, tenantId).isEmpty())
            throw ApiException.notFound("Department not found");
        Set<UUID> departmentTextIds = departmentId == null ? null : new HashSet<>(
                textDepartments.findAllByDepartmentIdIn(List.of(departmentId)).stream()
                        .map(TextDepartment::getTenantTextId).toList());
        List<RegisterRow> rows = new ArrayList<>();
        for (TenantText tenantText : tenantTexts.findAllByTenantId(tenantId)) {
            if (departmentTextIds != null && !departmentTextIds.contains(tenantText.getId())) continue;
            LawText lawText = lawTexts.findById(tenantText.getLawTextId()).orElse(null);
            if (lawText == null) continue;
            if (standardCode != null && lawText.getStandards().stream()
                    .noneMatch(standard -> standard.getCode().equalsIgnoreCase(standardCode))) continue;
            List<ActionItem> relatedActions = actions.findAllByTenantIdAndTenantTextId(tenantId, tenantText.getId());
            int evidenceCount = evidence.findAllByTenantIdAndTenantTextId(tenantId, tenantText.getId()).size();
            for (ActionItem action : relatedActions) evidenceCount += evidence.findAllByTenantIdAndActionId(tenantId, action.getId()).size();
            rows.add(new RegisterRow(lawText.getReference(), lawText.getTitleFr(), tenantText.getApplicability(),
                    tenantText.getConformity(), tenantText.getVerifiedOn() == null ? "" : tenantText.getVerifiedOn().toString(),
                    relatedActions.stream().map(ActionItem::getTitle).toList(), evidenceCount));
        }
        rows.sort(Comparator.comparing(RegisterRow::reference));
        return rows;
    }

    public byte[] excel(List<RegisterRow> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Registre conformité");
            Row header = sheet.createRow(0);
            String[] labels = {"Référence", "Texte", "Applicabilité", "Conformité", "Vérifié le", "Actions", "Preuves"};
            for (int i = 0; i < labels.length; i++) header.createCell(i).setCellValue(labels[i]);
            int index = 1;
            for (RegisterRow item : rows) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(item.reference());
                row.createCell(1).setCellValue(item.title());
                row.createCell(2).setCellValue(item.applicability());
                row.createCell(3).setCellValue(item.conformity());
                row.createCell(4).setCellValue(item.verifiedOn());
                row.createCell(5).setCellValue(String.join("; ", item.actions()));
                row.createCell(6).setCellValue(item.evidenceCount());
            }
            for (int i = 0; i < labels.length; i++) sheet.autoSizeColumn(i);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel report generation failed", e);
        }
    }

    public byte[] pdf(List<RegisterRow> rows, String reportTitle) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            int index = 0;
            while (index < rows.size() || index == 0) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(bold, 14);
                    content.newLineAtOffset(45, 755);
                    content.showText(safePdf(reportTitle));
                    content.setFont(regular, 8);
                    content.newLineAtOffset(0, -18);
                    content.showText("Vigilex — Généré le " + java.time.LocalDate.now());
                    content.newLineAtOffset(0, -24);
                    int pageEnd = Math.min(rows.size(), index + 35);
                    for (; index < pageEnd; index++) {
                        RegisterRow row = rows.get(index);
                        String line = row.reference() + " | " + row.title() + " | " + row.applicability()
                                + " | " + row.conformity() + " | Actions: " + row.actions().size()
                                + " | Preuves: " + row.evidenceCount();
                        content.showText(safePdf(line.length() > 125 ? line.substring(0, 122) + "..." : line));
                        content.newLineAtOffset(0, -17);
                    }
                    content.endText();
                }
                if (rows.isEmpty()) break;
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("PDF report generation failed", e);
        }
    }

    public byte[] csv(List<RegisterRow> rows) {
        StringBuilder csv = new StringBuilder("reference,title,applicability,conformity,verified_on,actions,evidence_count\r\n");
        for (RegisterRow row : rows) {
            csv.append(csvField(row.reference())).append(',')
                    .append(csvField(row.title())).append(',')
                    .append(csvField(row.applicability())).append(',')
                    .append(csvField(row.conformity())).append(',')
                    .append(csvField(row.verifiedOn())).append(',')
                    .append(csvField(String.join("; ", row.actions()))).append(',')
                    .append(row.evidenceCount()).append("\r\n");
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csvField(String value) {
        String safe = value == null ? "" : value;
        if (safe.matches("^[=+@\\-].*")) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String safePdf(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    public record RegisterRow(String reference, String title, String applicability, String conformity,
                              String verifiedOn, List<String> actions, int evidenceCount) { }
}
