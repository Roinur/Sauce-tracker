package com.roinur.saucetracker.feature.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniqueTrendScorerTest {
    @Test
    fun `supported distinctive shape outranks isolated read noise`() {
        val commonA = trend(1, listOf(2, 4, 7, 9, 8, 6, 4, 2))
        val commonB = trend(2, listOf(1, 3, 6, 8, 7, 5, 3, 1))
        val distinctive = trend(3, listOf(2, 3, 8, 15, 13, 5, 9, 14))
        val isolated = trend(4, listOf(0, 0, 0, 1, 0, 0, 0, 0))
        val all = listOf(commonA, commonB, distinctive, isolated)
        val scores = scoreUniqueTrends(all, all.associate { it.target.id to rawValues(it) })

        val distinctiveScore = scores.first { it.targetId == 3L }
        val isolatedScore = scores.first { it.targetId == 4L }
        assertTrue(distinctiveScore.uniqueScore > isolatedScore.uniqueScore)
        assertTrue(3L in strongestUniqueTrendIds(scores))
        assertTrue(4L !in strongestUniqueTrendIds(scores))
    }

    @Test
    fun `flat trends receive no shape novelty`() {
        val flat = trend(9, List(8) { 5 })
        val score = scoreUniqueTrends(listOf(flat), mapOf(9L to rawValues(flat))).single()
        assertEquals(0f, score.shapeNovelty, 0.0001f)
        assertEquals(0f, score.uniqueScore, 0.0001f)
    }

    @Test
    fun `important ordinary and unusual supported trends both survive`() {
        val scores = listOf(
            selectionScore(id = 1, reads = 200, significance = 1f, novelty = 0.20f),
            selectionScore(id = 2, reads = 80, significance = 0.75f, novelty = 0.15f),
            selectionScore(id = 3, reads = 25, significance = 0.40f, novelty = 1f),
            selectionScore(id = 4, reads = 2, significance = 0.10f, novelty = 0.95f)
        )
        val selection = selectMeaningfulUniqueTrends(scores, maximumCount = 3)

        assertTrue(1L in selection.targetIds)
        assertTrue(3L in selection.targetIds)
        assertTrue(4L !in selection.targetIds)
        assertTrue(
            selection.scores.first { it.targetId == 1L }.finalCandidateReason in setOf(
                UniqueTrendCandidateReason.CORE_SIGNIFICANCE,
                UniqueTrendCandidateReason.CORE_AND_METRIC,
                UniqueTrendCandidateReason.CORE_AND_NOVELTY,
                UniqueTrendCandidateReason.ALL
            )
        )
        assertTrue(
            selection.scores.first { it.targetId == 3L }.finalCandidateReason in setOf(
                UniqueTrendCandidateReason.SHAPE_NOVELTY,
                UniqueTrendCandidateReason.METRIC_AND_NOVELTY,
                UniqueTrendCandidateReason.CORE_AND_NOVELTY,
                UniqueTrendCandidateReason.ALL
            )
        )
    }

    @Test
    fun `supported high and low ratings outrank neutral and sparse extremes`() {
        val high = ratedTrend(10, rating = 5, ratingsPerBucket = 10)
        val low = ratedTrend(11, rating = 1, ratingsPerBucket = 10)
        val neutral = ratedTrend(12, rating = 3, ratingsPerBucket = 10)
        val sparseHigh = ratedTrend(13, rating = 5, ratingsPerBucket = 1)
        val all = listOf(high, low, neutral, sparseHigh)
        val scores = scoreUniqueTrends(
            series = all,
            valuesByTargetId = all.associate { it.target.id to trendValues(it, TrendScale.READS, TrendSignal.AVERAGE_RATING) },
            scale = TrendScale.READS,
            signal = TrendSignal.AVERAGE_RATING,
            ratingAdjustment = RatingAdjustment.BALANCED
        ).associateBy { it.targetId }

        assertTrue(scores.getValue(10L).metricStandoutScore > scores.getValue(12L).metricStandoutScore)
        assertTrue(scores.getValue(11L).metricStandoutScore > scores.getValue(12L).metricStandoutScore)
        assertTrue(scores.getValue(10L).metricStandoutScore > scores.getValue(13L).metricStandoutScore)
    }

    @Test
    fun `positive standout rewards supported rate over one of one`() {
        val sparse = positiveTrend(20, positive = 1, rated = 1)
        val supported = positiveTrend(21, positive = 40, rated = 45)
        val baseline = positiveTrend(22, positive = 20, rated = 45)
        val all = listOf(sparse, supported, baseline)
        val scores = scoreUniqueTrends(
            series = all,
            valuesByTargetId = all.associate { it.target.id to trendValues(it, TrendScale.SHARE, TrendSignal.POSITIVE) },
            significanceMode = UniqueTrendSignificanceMode.SHARE,
            scale = TrendScale.SHARE,
            signal = TrendSignal.POSITIVE
        ).associateBy { it.targetId }

        assertTrue(scores.getValue(21L).metricStandoutScore > scores.getValue(20L).metricStandoutScore)
        assertTrue(scores.getValue(20L).sampleCount < 3)
    }

    @Test
    fun `selection fills maximum when significance and novelty overlap`() {
        val scores = (1L..30L).mapIndexed { index, id ->
            val rank = 1f - (index / 40f)
            selectionScore(
                id = id,
                reads = 100 - index,
                significance = rank,
                novelty = rank
            )
        }

        val selection = selectMeaningfulUniqueTrends(scores, maximumCount = 24)

        assertEquals(24, selection.targetIds.size)
    }

    private fun selectionScore(
        id: Long,
        reads: Int,
        significance: Float,
        novelty: Float,
        metric: Float = 0f
    ) = UniqueTrendScore(
        targetId = id,
        sampleCount = reads,
        totalReadCount = reads,
        ratedCount = reads,
        readCount = reads,
        share = 0f,
        significanceScore = significance,
        significancePercentile = significance,
        rawMetricValue = metric,
        adjustedMetricValue = metric,
        metricStandoutScore = metric,
        metricStandoutPercentile = metric,
        shapeNovelty = novelty,
        noveltyPercentile = novelty,
        confidence = 1f,
        uniqueScore = novelty,
        combinedRank = maxOf(significance, metric, novelty)
    )

    private fun ratedTrend(id: Long, rating: Int, ratingsPerBucket: Int): TrendSeries {
        val ratings = IntArray(5).also { it[rating - 1] = ratingsPerBucket }
        return TrendSeries(
            target = TrendTarget(id, "rated-$id", "tag", ratingsPerBucket * 4),
            points = List(4) { index ->
                trendPoint(
                    bucket = index.toString(),
                    matching = ratingsPerBucket,
                    positive = if (rating >= 4) ratingsPerBucket else 0,
                    rated = ratingsPerBucket,
                    ratings = ratings
                )
            }
        )
    }

    private fun positiveTrend(id: Long, positive: Int, rated: Int): TrendSeries = TrendSeries(
        target = TrendTarget(id, "positive-$id", "tag", rated),
        points = listOf(
            trendPoint(
                bucket = "0",
                matching = rated,
                positive = positive,
                rated = rated,
                ratings = intArrayOf(rated - positive, 0, 0, positive, 0)
            ),
            trendPoint(bucket = "1")
        )
    )

    private fun trendPoint(
        bucket: String,
        matching: Int = 0,
        positive: Int = 0,
        rated: Int = 0,
        ratings: IntArray = IntArray(5)
    ) = TrendPoint(
        bucketKey = bucket,
        matchingReads = matching,
        totalReads = matching,
        positiveRatings = positive,
        ratedEntries = rated,
        ratingSum = ratings.indices.sumOf { (it + 1) * ratings[it] }.toFloat(),
        averageRating = if (rated > 0) ratings.indices.sumOf { (it + 1) * ratings[it] }.toFloat() / rated else 0f,
        rating1Count = ratings[0],
        rating2Count = ratings[1],
        rating3Count = ratings[2],
        rating4Count = ratings[3],
        rating5Count = ratings[4]
    )

    private fun trend(id: Long, reads: List<Int>): TrendSeries = TrendSeries(
        target = TrendTarget(id, "target-$id", "tag", reads.sum()),
        points = reads.mapIndexed { index, readCount ->
            TrendPoint(
                bucketKey = index.toString(),
                matchingReads = readCount,
                totalReads = readCount.coerceAtLeast(1),
                positiveRatings = 0,
                ratedEntries = 0,
                ratingSum = 0f,
                averageRating = 0f,
                rating1Count = 0,
                rating2Count = 0,
                rating3Count = 0,
                rating4Count = 0,
                rating5Count = 0
            )
        }
    )

    private fun rawValues(series: TrendSeries): List<Float> = series.points.map { it.matchingReads.toFloat() }
}
