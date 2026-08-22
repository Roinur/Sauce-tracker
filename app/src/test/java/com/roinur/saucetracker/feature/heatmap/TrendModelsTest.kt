package com.roinur.saucetracker.feature.heatmap

import com.roinur.saucetracker.ReadCountBreakdown
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TrendModelsTest {
    @Test
    fun `share carries forward across a period without reads`() {
        val values = trendValues(
            series(
                point("2026-08-01", matching = 1, total = 1),
                point("2026-08-02", matching = 0, total = 1),
                point("2026-08-03", matching = 0, total = 0)
            ),
            TrendScale.SHARE,
            TrendSignal.ALL
        )

        assertEquals(listOf(100f, 50f, 50f), values)
    }

    @Test
    fun `share average rating is cumulative and carries forward`() {
        val values = trendValues(
            series(
                point("2026-08-01", rated = 1, ratingSum = 4f, average = 4f, ratings = intArrayOf(0, 0, 0, 1, 0)),
                point("2026-08-02", rated = 1, ratingSum = 5f, average = 5f, ratings = intArrayOf(0, 0, 0, 0, 1)),
                point("2026-08-03")
            ),
            TrendScale.SHARE,
            TrendSignal.AVERAGE_RATING
        )

        assertEquals(4f, values[0], 0.0001f)
        assertEquals(4.5f, values[1], 0.0001f)
        assertEquals(4.5f, values[2], 0.0001f)
    }

    @Test
    fun `reads signals remain per period`() {
        val trend = series(
            point(
                "2026-08-01",
                matching = 3,
                total = 12,
                positive = 2,
                rated = 3,
                ratingSum = 12f,
                average = 4f,
                ratings = intArrayOf(0, 0, 1, 1, 1)
            )
        )

        assertEquals(listOf(3f), trendValues(trend, TrendScale.READS, TrendSignal.ALL))
        assertEquals(listOf(2f), trendValues(trend, TrendScale.READS, TrendSignal.POSITIVE))
        assertEquals(listOf(4f), trendValues(trend, TrendScale.READS, TrendSignal.AVERAGE_RATING))
    }

    @Test
    fun `per period average rating carries through empty buckets instead of becoming zero`() {
        val values = trendValues(
            series(
                point(
                    "2026-08-01",
                    rated = 1,
                    ratingSum = 5f,
                    average = 5f,
                    ratings = intArrayOf(0, 0, 0, 0, 1)
                ),
                point("2026-08-02")
            ),
            TrendScale.READS,
            TrendSignal.AVERAGE_RATING
        )

        assertEquals(listOf(5f, 5f), values)
    }

    @Test
    fun `trend requests use adaptive buckets unless debug explicitly asks for legacy`() {
        val request = TrendRequest(
            targetKind = TrendTargetKind.TAGS,
            targetIds = listOf(1L),
            range = com.roinur.saucetracker.StatsRange.MONTH
        )

        assertEquals(TrendBucketMode.ADAPTIVE, request.bucketMode)
    }

    @Test
    fun `thirty day rate uses only observed days in first and current long bucket`() {
        val factor = thirtyDayRateFactor(
            bucketStart = LocalDate.parse("2026-01-01"),
            bucketEndExclusive = LocalDate.parse("2026-04-01"),
            earliestObservedDate = LocalDate.parse("2026-02-01"),
            today = LocalDate.parse("2026-02-14")
        )

        assertEquals(30f / 14f, factor, 0.0001f)
    }

    @Test
    fun `long adaptive buckets expose a comparable thirty day read rate`() {
        val trend = series(
            point("2026-Q1", matching = 30, positive = 12, normalization = 1f / 3f)
        )

        assertEquals(listOf(10f), trendValues(trend, TrendScale.READS, TrendSignal.ALL))
        assertEquals(listOf(4f), trendValues(trend, TrendScale.READS, TrendSignal.POSITIVE))
    }

    @Test
    fun `curved rating adjustment emphasizes distance from neutral three`() {
        val trend = series(
            point(
                "2026-08-01",
                rated = 20,
                ratingSum = 50f,
                average = 2.5f,
                ratings = intArrayOf(10, 0, 0, 10, 0)
            )
        )

        val linear = trendValues(trend, TrendScale.READS, TrendSignal.AVERAGE_RATING, RatingAdjustment.LINEAR)
        val curved = trendValues(trend, TrendScale.READS, TrendSignal.AVERAGE_RATING, RatingAdjustment.BALANCED)

        assertEquals(2.5f, linear.single(), 0.0001f)
        assertEquals(2.464f, curved.single(), 0.001f)
    }

    @Test
    fun `reading breakdown totals unique entries and rereads`() {
        val breakdown = ReadCountBreakdown(uniqueEntries = 481, rereads = 1)

        assertEquals(482, breakdown.total)
    }

    private fun series(vararg points: TrendPoint) = TrendSeries(
        target = TrendTarget(1L, "Example", "tag", 1),
        points = points.toList()
    )

    private fun point(
        bucket: String,
        matching: Int = 0,
        total: Int = 0,
        positive: Int = 0,
        rated: Int = 0,
        ratingSum: Float = 0f,
        average: Float = 0f,
        ratings: IntArray = IntArray(5),
        normalization: Float = 1f
    ) = TrendPoint(
        bucketKey = bucket,
        matchingReads = matching,
        totalReads = total,
        positiveRatings = positive,
        ratedEntries = rated,
        ratingSum = ratingSum,
        averageRating = average,
        rating1Count = ratings[0],
        rating2Count = ratings[1],
        rating3Count = ratings[2],
        rating4Count = ratings[3],
        rating5Count = ratings[4],
        readNormalizationFactor = normalization
    )
}
