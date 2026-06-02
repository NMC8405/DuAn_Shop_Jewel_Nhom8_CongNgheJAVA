package com.jewelryshop.controller;

import com.jewelryshop.entity.Product;
import com.jewelryshop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    @Autowired
    private ProductRepository productRepository;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleChatMessage(@RequestBody Map<String, String> payload) {
        String userMsg = payload.getOrDefault("message", "").trim();
        Map<String, Object> response = new HashMap<>();

        if (userMsg.isEmpty()) {
            response.put("reply", "Xin chào! LuxeJewel Advisor có thể giúp gì cho bạn hôm nay?");
            return ResponseEntity.ok(response);
        }

        // Try using Google Gemini API first if key is configured
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                String geminiReply = callGeminiApi(userMsg);
                if (geminiReply != null && !geminiReply.isBlank()) {
                    response.put("reply", geminiReply);
                    response.put("suggestedProducts", new ArrayList<>()); // Gemini handles suggestions natively
                    return ResponseEntity.ok(response);
                }
            } catch (Exception e) {
                System.err.println("Gemini API Error: " + e.getMessage() + ". Falling back to local cognitive engine.");
            }
        }

        // Falls back to extremely smart, conversational local cognitive engine
        return ResponseEntity.ok(handleLocalCognitiveFallback(userMsg));
    }

    /**
     * Call the official Google Gemini API (gemini-1.5-flash) dynamically using RestTemplate
     */
    private String callGeminiApi(String userMessage) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey.trim();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Fetch catalog context to feed into Gemini so it knows live inventory
        List<Product> products = productRepository.findAll().stream().filter(Product::isActive).collect(Collectors.toList());
        StringBuilder catalogContext = new StringBuilder("Danh sách sản phẩm live tại cửa hàng:\n");
        DecimalFormat df = new DecimalFormat("#,###");
        for (Product p : products) {
            catalogContext.append("- ID: ").append(p.getId())
                    .append(" | Tên: ").append(p.getName())
                    .append(" | Giá: ").append(df.format(p.getCurrentPrice())).append("đ")
                    .append(" | Chất liệu: ").append(p.getMaterial() != null ? p.getMaterial() : "Cao cấp")
                    .append(" | Kho: ").append(p.getStockQuantity()).append("\n");
        }

        // System guidelines for LuxeJewel Advisor persona
        String systemInstruction = "Bạn là 'LuxeJewel Advisor', một Trợ lý ảo tư vấn trang sức cao cấp thông minh, tự nhiên, và vô cùng duyên dáng của cửa hàng LuxeJewel.\n" +
                "QUY TẮC PHẢN HỒI:\n" +
                "1. Giao tiếp bằng Tiếng Việt thanh lịch, tinh tế. Xưng hô 'Dạ, LuxeJewel xin chào...' hoặc 'Dạ, thưa quý khách...'.\n" +
                "2. Cực kỳ thông minh, nhạy bén, biết ứng biến và hóm hỉnh. Nếu khách trêu đùa (ví dụ hỏi mua 1.000.000 cái nhẫn), hãy đùa vui lại một cách dí dỏm, lịch thiệp, sau đó định hướng khéo léo về nhu cầu thật.\n" +
                "3. Nắm rõ thông tin:\n" +
                "   - Vận chuyển: Miễn phí cho đơn hàng từ 2.000.000đ trở lên. Dưới 2 triệu phí ship đồng giá 30k.\n" +
                "   - Đổi trả: Hỗ trợ đổi trả miễn phí trong vòng 30 ngày.\n" +
                "   - Bảo hành: Miễn phí đánh bóng, làm sạch trang sức trọn đời.\n" +
                "   - Mã giảm giá: WELCOME10 (giảm 10% cho đơn đầu tiên), SAVE500K (giảm 500k cho đơn từ 5 triệu).\n" +
                "4. Dựa vào ngữ cảnh sản phẩm thật sau để giới thiệu chính xác mẫu có tại shop khi khách hỏi mua, kèm mức giá chính xác:\n" +
                catalogContext.toString() + "\n" +
                "Khi giới thiệu sản phẩm thật, hãy khuyên khách bấm nút 'Xem Chi Tiết' trên trang hoặc truy cập liên kết dạng /shop/ID để xem trực tiếp.";

        // Build Gemini API Request Payload
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", userMessage);
        Map<String, Object> contentObj = new HashMap<>();
        contentObj.put("parts", Collections.singletonList(textPart));
        requestBody.put("contents", Collections.singletonList(contentObj));

        Map<String, Object> systemPart = new HashMap<>();
        systemPart.put("text", systemInstruction);
        Map<String, Object> systemInstructionObj = new HashMap<>();
        systemInstructionObj.put("parts", Collections.singletonList(systemPart));
        requestBody.put("systemInstruction", systemInstructionObj);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
            Map body = responseEntity.getBody();
            List candidates = (List) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                if (content != null) {
                    List parts = (List) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map firstPart = (Map) parts.get(0);
                        return (String) firstPart.get("text");
                    }
                }
            }
        }
        return null;
    }

    /**
     * Conversational Local Fallback Engine with rich chitchat capability, jokes, and logical understanding
     */
    private Map<String, Object> handleLocalCognitiveFallback(String userMsg) {
        String lowerMsg = userMsg.toLowerCase();
        Map<String, Object> response = new HashMap<>();
        String reply;
        List<Map<String, Object>> suggestedProducts = new ArrayList<>();

        // A. Detection of extreme quantities or joke purchase requests (e.g. 1000000 nhẫn)
        if (containsAny(lowerMsg, "1000000", "một triệu", "mot trieu", "triệu cái", "tỷ cái", "ty cai", "nhiều nhẫn", "hết cửa hàng", "mua hết")) {
            reply = "😄 **Ôi trời! Quý khách muốn đặt mua 1.000.000 chiếc nhẫn sao ạ?**<br><br>" +
                    "Dạ, nếu làm vậy thì chắc LuxeJewel phải thầu trọn gói tất cả các mỏ kim cương đắt giá nhất trên toàn cầu mới kịp chế tác cho quý khách mất thôi! Nhưng quả thật LuxeJewel vô cùng vinh hạnh trước độ chịu chơi này của quý khách. ❤️<br><br>" +
                    "Đùa vui một chút, không biết quý khách đang có nhu cầu đặt hàng số lượng sỉ cho doanh nghiệp, hay đang muốn tìm kiếm một mẫu nhẫn cầu hôn, nhẫn cưới kim cương độc bản và lấp lánh nhất để dành tặng cho nửa kia của mình ạ?";
        }
        // B. Love life chitchat or personal identity queries
        else if (containsAny(lowerMsg, "người yêu", "nguoi yeu", "crush", "yêu ai", "yeu ai", "có bồ", "co bo")) {
            reply = "Dạ, là một Trợ lý ảo tư vấn trang sức cao cấp, trái tim tôi đã trót trao cho những viên đá quý lấp lánh và những chiếc nhẫn kim cương thiên nhiên tinh xảo tại cửa hàng mất rồi! 🥰<br><br>" +
                    "Còn quý khách thì sao ạ? Có phải quý khách đang tìm kiếm một món quà tinh tế như nhẫn cầu hôn, vòng cổ vàng để bày tỏ tấm chân tình với người thương của mình không? Hãy kể cho tôi nghe để tôi lựa chọn giúp quý khách nhé!";
        }
        // C. AI creator question
        else if (containsAny(lowerMsg, "ai tạo", "ai tao", "sáng lập", "sang lap", "kỹ sư", "ky su", "engineer", "creator")) {
            reply = "Dạ, tôi được phát triển bởi đội ngũ kỹ sư phần mềm tài năng của LuxeJewel, với mong muốn mang lại trải nghiệm mua sắm trang sức trực tuyến đẳng cấp, thông minh và thuận tiện nhất cho quý khách hàng thân yêu! 💻✨<br><br>" +
                    "Tôi luôn túc trực 24/7 tại đây để giải đáp mọi băn khoăn của quý khách về nhẫn, bông tai, dây chuyền hoặc cách bảo quản trang sức luôn sáng đẹp như mới.";
        }
        // D. General greetings
        else if (lowerMsg.contains("chào") || lowerMsg.contains("hello") || lowerMsg.contains("hi ") || lowerMsg.equals("hi") || lowerMsg.contains("bạn là ai")) {
            reply = "✨ **Xin chào quý khách hàng thân yêu!** Tôi là Trợ lý tư vấn trang sức thông minh LuxeJewel Advisor. ✨<br><br>" +
                    "Tôi có thể hỗ trợ quý khách:<br>" +
                    "• 🔍 **Tìm kiếm & Tư vấn trang sức** (nhẫn cưới, dây chuyền, bông tai, vòng tay...)<br>" +
                    "• 📏 **Hướng dẫn đo size ngón tay** cực chuẩn xác tại nhà.<br>" +
                    "• ✨ **Chia sẻ bí quyết bảo quản** vàng, bạc, kim cương luôn lấp lánh.<br>" +
                    "• 🏷️ **Cung cấp mã giảm giá hot** hiện có.<br><br>" +
                    "Quý khách đang tìm kiếm trang sức cho sự kiện đặc biệt nào để tôi lựa chọn giúp ạ?";
        }
        // E. Dynamic Recommendation by category/materials
        else if (containsAny(lowerMsg, "nhẫn", "nhan", "dây chuyền", "day chuyen", "vòng cổ", "vong co", "bông tai", "khuyên tai", "bong tai", "khuyen tai", "vòng tay", "vong tay", "lắc chân", "lac chan", "kim cương", "kim cuong", "ruby", "ngọc trai", "ngoc trai", "vàng", "vang", "bạc", "bac", "emerald", "lục bảo", "mua gì", "tư vấn", "tu van")) {
            List<Product> allProducts = productRepository.findAll().stream().filter(Product::isActive).collect(Collectors.toList());
            List<Product> matched = new ArrayList<>();

            if (containsAny(lowerMsg, "nhẫn", "nhan")) {
                matched = filterProducts(allProducts, "Nhẫn");
            } else if (containsAny(lowerMsg, "dây chuyền", "day chuyen", "vòng cổ", "vong co")) {
                matched = filterProducts(allProducts, "Dây Chuyền");
            } else if (containsAny(lowerMsg, "bông tai", "khuyên tai", "bong tai", "khuyen tai")) {
                matched = filterProducts(allProducts, "Bông Tai");
            } else if (containsAny(lowerMsg, "vòng tay", "vong tay", "lắc tay", "lac tay")) {
                matched = filterProducts(allProducts, "Vòng Tay");
            } else if (containsAny(lowerMsg, "lắc chân", "lac chan")) {
                matched = filterProducts(allProducts, "Lắc Chân");
            }

            if (matched.isEmpty() || containsAny(lowerMsg, "kim cương", "kim cuong", "ruby", "ngọc trai", "ngoc trai", "lục bảo", "emerald", "vàng", "vang", "bạc", "bac")) {
                final List<Product> base = matched.isEmpty() ? allProducts : matched;
                if (containsAny(lowerMsg, "kim cương", "kim cuong")) {
                    matched = filterByKeyword(base, "kim cương");
                } else if (containsAny(lowerMsg, "ruby")) {
                    matched = filterByKeyword(base, "ruby");
                } else if (containsAny(lowerMsg, "ngọc trai", "ngoc trai")) {
                    matched = filterByKeyword(base, "ngọc trai");
                } else if (containsAny(lowerMsg, "lục bảo", "emerald")) {
                    matched = filterByKeyword(base, "emerald");
                } else if (containsAny(lowerMsg, "vàng", "vang")) {
                    matched = filterByKeyword(base, "vàng");
                } else if (containsAny(lowerMsg, "bạc", "bac")) {
                    matched = filterByKeyword(base, "bạc");
                }
            }

            if (!matched.isEmpty()) {
                int limit = Math.min(matched.size(), 3);
                DecimalFormat df = new DecimalFormat("#,###");
                StringBuilder sb = new StringBuilder();
                sb.append("LuxeJewel sở hữu các tác phẩm tinh tuyển vô cùng phù hợp với gu thẩm mỹ tinh tế của quý khách:<br><br>");

                for (int i = 0; i < limit; i++) {
                    Product p = matched.get(i);
                    String priceStr = df.format(p.getCurrentPrice()) + "đ";

                    sb.append("✨ **").append(p.getName()).append("**<br>");
                    if (p.getMaterial() != null) {
                        sb.append("• Chất liệu: *").append(p.getMaterial()).append("* | ");
                    }
                    sb.append("Giá đặc biệt: <span class=\"text-gold fw-bold\">").append(priceStr).append("</span><br>");
                    sb.append("<a href=\"/shop/").append(p.getId()).append("\" target=\"_blank\" class=\"btn btn-xs btn-gold mt-1 py-1 px-2 d-inline-block text-decoration-none\" style=\"font-size: 0.72rem;\"><i class=\"fas fa-eye me-1\"></i>Xem Chi Tiết</a><br><br>");

                    Map<String, Object> card = new HashMap<>();
                    card.put("id", p.getId());
                    card.put("name", p.getName());
                    card.put("price", priceStr);
                    card.put("image", p.getMainImage());
                    card.put("url", "/shop/" + p.getId());
                    suggestedProducts.add(card);
                }
                sb.append("Quý khách có muốn tôi tư vấn sâu hơn về giác cắt đá quý hay chính sách giao hàng hỏa tốc tận nhà không ạ?");
                reply = sb.toString();
            } else {
                reply = "Dạ, hiện các mẫu này của cửa hàng đang trong thời gian hoàn thiện thiết kế mới hoặc tạm hết hàng. Quý khách có muốn tham khảo những tuyệt phẩm **Nhẫn Kim Cương Vàng 18K** luôn có sẵn của LuxeJewel không ạ?";
            }
        }
        // F. Size advice
        else if (containsAny(lowerMsg, "size", "kích cỡ", "kich co", "kích thước", "kich thuoc", "đo", "do size")) {
            reply = "📏 **Cách đo size nhẫn chuẩn xác 100% tại nhà:**<br><br>" +
                    "**1.** Quấn một sợi chỉ mảnh hoặc mẩu giấy ngang bản 5mm quanh ngón tay đeo nhẫn của bạn.<br>" +
                    "**2.** Đánh dấu điểm giao nhau và dùng thước đo chính xác độ dài bằng milimet (đó là chu vi).<br>" +
                    "**3.** Chia số chu vi đó cho **3.14** để tính ra đường kính lòng nhẫn.<br><br>" +
                    "**BẢNG SIZE THAM KHẢO NHANH:**<br>" +
                    "• Chu vi 50-52mm: Size nhẫn 10-12<br>" +
                    "• Chu vi 53-55mm: Size nhẫn 13-15<br>" +
                    "• Chu vi 56-58mm: Size nhẫn 16-18<br><br>" +
                    "Gửi cho tôi chu vi ngón tay của quý khách, tôi sẽ tính ngay size cho quý khách nhé!";
        }
        // G. Cleaning advice
        else if (containsAny(lowerMsg, "bảo quản", "bao quan", "làm sạch", "lam sach", "vệ sinh", "ve sinh", "rửa")) {
            reply = "✨ **Bí quyết giữ trang sức luôn lấp lánh như lúc mới mua:**<br><br>" +
                    "• **Trang sức Vàng & Kim Cương**: Ngâm vào chén nước ấm pha loãng chút dầu gội nhẹ trong 10 phút, chà nhẹ bằng bàn chải lông mềm và lau khô bằng khăn không xơ.<br>" +
                    "• **Trang sức Bạc 925**: Ngâm trong nước ấm pha chút nước giấm và muối trong 15 phút để loại bỏ hoàn toàn các vết xỉn đen tự nhiên do mồ hôi.<br><br>" +
                    "Tránh xịt nước hoa hoặc thoa mỹ phẩm trực tiếp lên bề mặt đá quý để bảo vệ độ trong suốt của đá quý khách nhé!";
        }
        // H. Coupons and promotions
        else if (containsAny(lowerMsg, "khuyến mãi", "khuyen mai", "giảm giá", "giam gia", "coupon", "voucher", "ưu đãi")) {
            reply = "🏷️ **Ưu đãi hấp dẫn đang kích hoạt tại LuxeJewel:**<br><br>" +
                    "• **Mã `WELCOME10`**: Giảm ngay 10% giá trị sản phẩm Nhẫn & Vòng cổ mới.<br>" +
                    "• **Mã `SAVE500K`**: Chiết khấu trực tiếp 500,000đ cho hóa đơn vàng/kim cương từ 10 triệu đồng trở lên.<br>" +
                    "• **Vận chuyển**: Miễn phí vận chuyển hỏa tốc toàn quốc cho tất cả các đơn từ 2.000.000đ.<br><br>" +
                    "Quý khách đừng quên áp dụng mã ưu đãi ở bước thanh toán nhé!";
        }
        // I. Default fallback
        else {
            reply = "Dạ, LuxeJewel Advisor luôn sẵn lòng giải đáp mọi chia sẻ của quý khách! Quý khách có thể hỏi tôi về các mẫu trang sức thiết kế cao cấp, hướng dẫn cách đo kích cỡ ngón tay đeo nhẫn, hay các chương trình ưu đãi hiện tại nhé. Quý khách muốn trò chuyện thêm về chủ đề gì ạ?";
        }

        response.put("reply", reply);
        response.put("suggestedProducts", suggestedProducts);
        return response;
    }

    private boolean containsAny(String input, String... keywords) {
        for (String kw : keywords) {
            if (input.contains(kw)) return true;
        }
        return false;
    }

    private List<Product> filterProducts(List<Product> list, String categoryName) {
        return list.stream()
                .filter(p -> p.getCategory() != null && categoryName.equalsIgnoreCase(p.getCategory().getName()))
                .collect(Collectors.toList());
    }

    private List<Product> filterByKeyword(List<Product> list, String keyword) {
        return list.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword) || 
                            (p.getMaterial() != null && p.getMaterial().toLowerCase().contains(keyword)) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(keyword)))
                .collect(Collectors.toList());
    }
}
