package faang.school.urlshortenerservice.service;

import faang.school.urlshortenerservice.cache.HashCache;
import faang.school.urlshortenerservice.dto.UrlDto;
import faang.school.urlshortenerservice.entity.Hash;
import faang.school.urlshortenerservice.entity.Url;
import faang.school.urlshortenerservice.exception.UrlNotFoundException;
import faang.school.urlshortenerservice.repository.UrlCacheRepository;
import faang.school.urlshortenerservice.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlCacheRepository urlCacheRepository;
    private final HashCache hashCache;
    private final UrlRepository urlRepository;

    @Transactional
    public String shortenUrl(UrlDto url) {
        Hash hash = hashCache.getHash();
        urlRepository.save(new Url(hash.getHash(), url.getUrl(), LocalDateTime.now()));
        urlCacheRepository.save(hash.getHash(), url.getUrl());
        log.info("URL сохранен в БД и кэше: {}", url.getUrl());
        return hash.getHash();
    }

    public String getOriginalUrl(String hash) {
        log.info("Получили запрос на получение оригинальной ссылки по хэшу: {}", hash);

        return urlCacheRepository.get(hash) // Попробуем найти URL в кэше.
                .orElseGet(() -> {
                    log.info("URL не найден в кэше: {}", hash);
                    Url urlFromDb = urlRepository.findByHash(hash)    // Если URL не найден в кэше, ищем в базе данных.
                            .orElseThrow(() -> new UrlNotFoundException(
                                    String.format("URL по хешу не найден ни в кэше, ни в БД: %s", hash)
                            ));
                    log.info("URL найден в БД: {}", hash);

                    urlCacheRepository.save(hash, urlFromDb.getUrl()); // Найденный URL в базе данных сохраняем в кэш для будущего использования.
                    log.info("URL сохранен в кэше: {}", hash);
                    return urlFromDb.getUrl();
                });
    }

}