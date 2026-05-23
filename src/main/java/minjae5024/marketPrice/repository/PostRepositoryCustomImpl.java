package minjae5024.marketPrice.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import minjae5024.marketPrice.entity.Post;
import minjae5024.marketPrice.entity.QMarket;
import minjae5024.marketPrice.entity.QPost;
import minjae5024.marketPrice.entity.QUser;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> search(Long marketId, String searchType, String keyword, Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QMarket market = QMarket.market;

        List<Post> results = queryFactory
                .selectFrom(post)
                .join(post.user, user).fetchJoin()
                .join(post.market, market).fetchJoin()
                .where(
                        marketIdEq(marketId),
                        searchPredicate(searchType, keyword)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifiers(pageable.getSort())) 
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        marketIdEq(marketId),
                        searchPredicate(searchType, keyword)
                );

        return PageableExecutionUtils.getPage(results, pageable, () -> {
            Long count = countQuery.fetchOne();
            return count != null ? count : 0L;
        });
    }

    private BooleanExpression marketIdEq(Long marketId) {
        return marketId != null ? QPost.post.market.id.eq(marketId) : null;
    }

    private BooleanExpression searchPredicate(String searchType, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        if ("title".equalsIgnoreCase(searchType)) {
            return QPost.post.title.containsIgnoreCase(keyword);
        }
        if ("content".equalsIgnoreCase(searchType)) { 
            return QPost.post.title.containsIgnoreCase(keyword).or(QPost.post.content.containsIgnoreCase(keyword));
        }
        if ("author".equalsIgnoreCase(searchType)) {
            return QPost.post.user.username.containsIgnoreCase(keyword);
        }
        return null;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        if (sort.isUnsorted()) {
            return new OrderSpecifier[0];
        }

        return sort.stream()
                .map(order -> {
                    PathBuilder<Post> pathBuilder = new PathBuilder<>(Post.class, "post");
                    return new OrderSpecifier(order.isAscending() ? Order.ASC : Order.DESC,
                            pathBuilder.get(order.getProperty()));
                })
                .toArray(OrderSpecifier[]::new);
    }
}
