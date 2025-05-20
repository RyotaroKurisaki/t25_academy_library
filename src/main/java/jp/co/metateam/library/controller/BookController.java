package jp.co.metateam.library.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.AccountDto;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.service.BookMstService;
import lombok.extern.log4j.Log4j2;

/**
 * 書籍関連クラス
 */
@Log4j2
@Controller
public class BookController {
    
    private final BookMstService bookMstService;

    @Autowired
    public BookController(BookMstService bookMstService){
        this.bookMstService = bookMstService;
    }

    @GetMapping("/book/index")
    public String index(Model model) {
        // 書籍を全件取得
        List<BookMstDto> bookMstList = this.bookMstService.findAvailableWithStockCount();
        
        model.addAttribute("bookMstList", bookMstList);

        return "book/index";
    }

    @GetMapping("/book/add")
    public String add(Model model) {
        if (!model.containsAttribute("bookMstDto")) {
            model.addAttribute("bookMstDto", new BookMstDto());
        }

        return "book/add";
    }

    @PostMapping("/book/add")
    public String register(@Valid BookMstDto bookMstDto, BindingResult result, RedirectAttributes ra, Model model) {
        try{

            boolean errIsbnFlg = false;
            if(result.hasErrors()){
                model.addAttribute("bookMstDto", bookMstDto);
                model.addAttribute("org.springframework.vailidation.BindingResult.bookMstDto", result);
                return "book/add";
            }

            BookMst isbnExist = this.bookMstService.selectByIsbn(bookMstDto.getIsbn());
            if(isbnExist != null){
                result.rejectValue("isbn", "error.value", "登録済みのISBNです");
                errIsbnFlg = true;
                return "book/add";
            }

            bookMstService.save(bookMstDto);

            return "redirect:/book/index";
        }catch (Exception e){
            log.error("登録失敗:"+e.getMessage() );
            log.error("書籍情報の保存に失敗しました",e);
            model.addAttribute("errorMessage","書類情報の保存中にエラーが発生しました。もう一度お試しください。");

            return "book/add";    
        }
    } 


   @GetMapping("/book/edit/{id}")
    public String editBook(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {

    Optional<BookMst> bookOpt = bookMstService.findById(id);

    if (bookOpt.isEmpty()) {
        ra.addFlashAttribute("errorMessage", "書籍が存在しません");
        return "redirect:/book/index";
    }

    BookMst book = bookOpt.get();

    if (book.getDeletedFlag() != null && book.getDeletedFlag()) {
        ra.addFlashAttribute("errorMessage", "削除済みの書籍です");
        return "redirect:/book/index";
    }

    BookMstDto dto = new BookMstDto();
    dto.setId(book.getId());
    dto.setIsbn(book.getIsbn());
    dto.setTitle(book.getTitle());

    model.addAttribute("bookMstDto", dto);
    return "book/edit";
}



    
   @PostMapping("/book/update")
    public String updateBook(
        @Valid @ModelAttribute("bookMstDto") BookMstDto bookMstDto, Long id,
        BindingResult result,
        Model model,
        RedirectAttributes ra
    ) {
    Optional<BookMst> bookOpt = bookMstService.findById(id);

    if (bookOpt.isEmpty()) {
        ra.addFlashAttribute("errorMessage", "書籍が存在しません");
        return "redirect:/book/index";
    }

    BookMst book = bookOpt.get();

    if (book.getDeletedFlag() != null && book.getDeletedFlag()) {
        ra.addFlashAttribute("errorMessage", "削除済みの書籍です");
        return "redirect:/book/index";
    }
    
    BookMst existing = bookOpt.get();
    


    // 入力値と既存データが同じならリダイレクト（変更なし）
    boolean isSame = existing.getIsbn().equals(bookMstDto.getIsbn())
                  && existing.getTitle().equals(bookMstDto.getTitle());
    if (isSame) {
        return "redirect:/book/index"; // メッセージなし
    }

    // ISBNの重複チェック（ISBNが変更された場合のみ）
    if (!existing.getIsbn().equals(bookMstDto.getIsbn())) {
        BookMst other = bookMstService.selectByIsbn(bookMstDto.getIsbn());
        if (other != null) {
            result.rejectValue("isbn", "error.value", "登録済みのISBNです");
        }
    }

    // バリデーションエラーがあれば戻る
    if (result.hasErrors()) {
        model.addAttribute("bookMstDto", bookMstDto);
        return "book/edit";
    }

    // 更新処理
    existing.setIsbn(bookMstDto.getIsbn());
    existing.setTitle(bookMstDto.getTitle());
    bookMstService.update(existing);

    ra.addFlashAttribute("message", "書籍情報が更新されました");
    return "redirect:/book/index";
    }

   @GetMapping("/book/delete/{id}")
public String deleteBook(@PathVariable("id") Long id, RedirectAttributes ra) {
    try {
        bookMstService.deleteById(id);
        ra.addFlashAttribute("message", "書籍を削除しました");
    } catch (IllegalArgumentException e) {
        ra.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/book/index";
}

    
}
