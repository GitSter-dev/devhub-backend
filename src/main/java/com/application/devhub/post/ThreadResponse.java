package com.application.devhub.post;

import java.util.List;

public record ThreadResponse(PostView post, List<PostView> ancestors) {
}
