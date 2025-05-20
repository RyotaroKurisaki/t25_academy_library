package jp.co.metateam.library.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.micrometer.common.util.StringUtils;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.repository.BookMstRepository;

@Service
public class BookMstService {

    private final BookMstRepository bookMstRepository;
    
    @Autowired
    public BookMstService(BookMstRepository bookMstRepository){
        this.bookMstRepository = bookMstRepository;
    }
    public BookMst selectByIsbn(String isbn){
        return bookMstRepository.selectByIsbn(isbn);
    }
    
    public List<BookMstDto> findAvailableWithStockCount() {
        List<BookMst> books = this.bookMstRepository.findLimitedBook();
        List<BookMstDto> bookMstDtoList = new ArrayList<BookMstDto>();

        // 書籍の在庫数を取得
        // FIXME: 現状は書籍ID毎にDBに問い合わせている。一度のSQLで完了させたい。
        for (int i = 0; i < books.size(); i++) {
            BookMst book = books.get(i);
            BookMstDto bookMstDto = new BookMstDto();
            bookMstDto.setId(book.getId());
            bookMstDto.setIsbn(book.getIsbn());
            bookMstDto.setTitle(book.getTitle());
            bookMstDtoList.add(bookMstDto);
        }

        return bookMstDtoList;
    }
    @Transactional
    public void save(BookMstDto bookMstDto){
        BookMst bookMst = new BookMst();
        bookMst.setIsbn(bookMstDto.getIsbn());
        bookMst.setTitle(bookMstDto.getTitle());
        bookMst.setDeletedFlag(Boolean.FALSE);

        this.bookMstRepository.save(bookMst);
    }
    public Optional<BookMst> findById(Long id) {
        return bookMstRepository.findById(id);
    }
    @Transactional
    public void update(BookMst bookMst) {
        bookMstRepository.save(bookMst); // saveはupdateも含む
    }
    @Transactional
    public void deleteById(Long id) {
        Optional<BookMst> bookOpt = bookMstRepository.findById(id);

        if (bookOpt.isEmpty()) {
            throw new IllegalArgumentException("書籍が存在しません");
        }

        BookMst book = bookOpt.get();

        if (book.getDeletedFlag() != null && book.getDeletedFlag()) {
            throw new IllegalArgumentException("削除済みの書籍です");
        }

        // 論理削除処理
        book.setDeletedFlag(true);
        book.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        bookMstRepository.save(book);
    }

}



