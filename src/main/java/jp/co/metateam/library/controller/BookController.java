package jp.co.metateam.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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


    @GetMapping("/book/edit/{id}")
    public String editBook(@PathVariable("id") Long id, RedirectAttributes ra, Model model) {
    BookMst book = bookMstService.selectById(id);
    
    if (book == null) {
        ra.addFlashAttribute("notFound", "書類が存在しません"); // 一覧画面に戻す
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
        @Valid @ModelAttribute("bookMstDto") BookMstDto bookMstDto,
        BindingResult result,
        Model model,
        RedirectAttributes ra
    ) {
    // 1. 編集対象の書籍を取得
    BookMst existing = bookMstService.selectById(bookMstDto.getId());

    if (existing == null) {
        // 存在しない → 一覧に戻ってメッセージ表示
        ra.addFlashAttribute("notFoundMessage", "書籍が存在しないため、操作をキャンセルしました。");
        return "redirect:/book/index";
    }

    // 2. ISBNが変更されていた場合の重複チェック
    if (!existing.getIsbn().equals(bookMstDto.getIsbn())) {
        BookMst isbnExist = bookMstService.selectByIsbn(bookMstDto.getIsbn());
        if (isbnExist != null) {
            result.rejectValue("isbn", "error.value", "登録済みのISBNです");
        }
    }

    // 3. バリデーションエラーがあれば編集画面に戻す
    if (result.hasErrors()) {
        model.addAttribute("bookMstDto", bookMstDto);
        return "book/edit";
    }

    // 4. ISBN・タイトルのどちらも変更されていなければ、一覧に戻って「変更なし」メッセージ表示
    boolean isUnchanged =
        existing.getIsbn().equals(bookMstDto.getIsbn()) &&
        existing.getTitle().equals(bookMstDto.getTitle());

    if (isUnchanged) {
        ra.addFlashAttribute("infoMessage", "変更内容がありませんでした。");
        return "redirect:/book/index";
    }

    // 5. 変更あり → 更新処理
    existing.setIsbn(bookMstDto.getIsbn());
    existing.setTitle(bookMstDto.getTitle());
    bookMstService.update(existing);

    ra.addFlashAttribute("successMessage", "書籍を更新しました。");
    return "redirect:/book/index";
}
    
     @GetMapping("/book/delete/{id}")
    public String deleteBook(@PathVariable("id") Long id, RedirectAttributes ra) {
       
        bookMstService.deleteById(id);
        ra.addFlashAttribute("deleteMessage", "書籍を削除しました");
        
        return "redirect:/book/index";
    }


    @PostMapping("/book/add")
    public String register(@Valid BookMstDto bookMstDto, BindingResult result, RedirectAttributes ra, Model model) {
        

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
          
        
    } 
  

}