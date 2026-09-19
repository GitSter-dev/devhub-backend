package com.application.devhub.post;

import java.util.List;

public record PostPage(List<PostView> items, String nextCursor) {
}
