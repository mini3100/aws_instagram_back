package com.toyproject.instagram.service;

import com.toyproject.instagram.dto.UploadFeedReqDto;
import com.toyproject.instagram.entity.Feed;
import com.toyproject.instagram.entity.FeedImg;
import com.toyproject.instagram.repository.FeedMapper;
import com.toyproject.instagram.security.PrincipalUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedMapper feedMapper;

    @Value("${file.path}")  // yml파일에서 path를 가져옴
    private String filePath;

    @Transactional( rollbackFor = Exception.class )     // 모든 예외 발생시 롤백
    public void upload(UploadFeedReqDto uploadFeedReqDto) {
        String content = uploadFeedReqDto.getContent();
        List<FeedImg> feedImgList = new ArrayList<>();
        PrincipalUser principalUser =
                (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = principalUser.getUsername();

        Feed feed = Feed.builder()
                .content(content)
                .username(username)
                .build();

        feedMapper.saveFeed(feed);

        uploadFeedReqDto.getFiles().forEach(file -> {
            String originName = file.getOriginalFilename(); // 파일 이름
            String extensionName = originName.substring(originName.lastIndexOf("."));   // 파일 확장자만 잘라줌
            String saveName = UUID.randomUUID().toString().replaceAll("-", "").concat(extensionName);    // 새로운 고유한 파일명 생성

            Path uploadPath = Paths.get(filePath + "/feed/" + saveName);  // 업로드할 파일 경로

            File f = new File(filePath + "/feed");
            if(!f.exists()) {   // 파일이 존재하지 않으면(예외 처리)
                f.mkdirs();     // 해당 경로를 만들어라.(feed 폴더가 없을 경우)
            }

            try {
                Files.write(uploadPath, file.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            feedImgList.add(FeedImg.builder()
                            .feedId(feed.getFeedId())
                            .originFileName(originName)
                            .saveFileName(saveName)
                            .build());
        });

        feedMapper.saveFeedImgList(feedImgList);
    }
}
