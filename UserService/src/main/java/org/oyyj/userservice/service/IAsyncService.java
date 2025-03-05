package org.oyyj.userservice.service;

import org.oyyj.blogservice.dto.BlogDTO;

public interface IAsyncService {
    void upLoadBlogToAI(BlogDTO blogDTO);
}
