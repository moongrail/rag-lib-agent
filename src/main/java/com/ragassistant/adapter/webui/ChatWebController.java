package com.ragassistant.adapter.webui;

import com.ragassistant.common.TenantContext;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.ingestion.DocumentIngestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import java.util.List;

@Controller
@Profile("webui")
public class ChatWebController {

    private final DocumentIngestionService ingestionService;

    public ChatWebController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/")
    public String index() {
        return "chat";
    }

    @PostMapping("/ui/documents")
    public String upload(@RequestParam("file") List<MultipartFile> files, RedirectAttributes attributes) {
        String tenant = TenantContext.get();
        try {
            List<DocumentMetadata> metas = ingestionService.ingestAll(files, tenant, tenant);
            long ok = metas.stream().filter(m -> m.getStatus() == DocumentStatus.INGESTED).count();
            attributes.addFlashAttribute("message",
                    "Ingested " + ok + "/" + metas.size() + " document(s).");
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Ingestion failed: " + e.getMessage());
        }
        return "redirect:/";
    }
}
