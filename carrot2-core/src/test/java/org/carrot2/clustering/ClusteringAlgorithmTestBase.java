package org.carrot2.clustering;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Nightly;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import org.carrot2.AbstractTest;
import org.carrot2.language.LanguageComponents;
import org.carrot2.language.TestsLanguageComponentsFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ClusteringAlgorithmTestBase extends AbstractTest {
  protected abstract ClusteringAlgorithm algorithm();

  protected LanguageComponents testLanguageModel() {
    return LanguageComponents.get(TestsLanguageComponentsFactory.NAME);
  }

  /**
   * A test to check if the algorithm does not fail with no documents.
   */
  @Test
  public void testNoDocuments() {
    assertThat(algorithm().cluster(Stream.empty(), testLanguageModel())).isEmpty();
  }

  @Test
  public void testDocumentsWithoutContent() {
    List<Document> documents = IntStream.range(0, randomIntBetween(1, 100))
        .mapToObj(i -> new Document() {
          @Override
          public void visitFields(BiConsumer<String, String> fieldConsumer) {
            // No fields.
          }
        })
        .collect(Collectors.toList());

    final List<Cluster<Document>> clusters = algorithm().cluster(
        documents.stream(),
        testLanguageModel());

    assertThat(clusters).isEmpty();
  }

  @Test
  public void testClusteringSampleDataSet() {
    List<Cluster<Document>> clusters = algorithm().cluster(
        SampleDocumentData.DOCUMENTS_DATA_MINING.stream(),
        testLanguageModel());
    assertThat(clusters.size())
        .isGreaterThan(0);
  }


  /**
   * Runs the algorithm concurrently, verifying stability of results.
   */
  @Nightly
  @Test
  @ThreadLeakLingering(linger = 5000)
  public void testResultsStable() throws Exception {
    final int numberOfThreads = randomIntBetween(1, 8);
    final int queriesPerThread = scaledRandomIntBetween(5, 25);

    System.out.println("Threads: " + numberOfThreads + ", qpt: " + queriesPerThread);

    List<Document> documents = RandomizedTest.randomFrom(Arrays.asList(
        SampleDocumentData.DOCUMENTS_DATA_MINING,
        SampleDocumentData.DOCUMENTS_DAWID));

    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    List<Callable<List<Cluster<Document>>>> callables = new ArrayList<>();
    for (int i = 0; i < numberOfThreads * queriesPerThread; i++) {
      final int dataSetIndex = i;
      callables.add(() -> {
        long s = System.currentTimeMillis();
        try {
          return algorithm().cluster(documents.stream(), testLanguageModel());
        } finally {
          System.out.println("Done. " + (System.currentTimeMillis() - s));
        }
      });
    }

    try {
      List<Cluster<Document>> reference = null;
      for (Future<List<Cluster<Document>>> f : executorService.invokeAll(callables)) {
        List<Cluster<Document>> clusters = f.get();
        if (reference == null) {
          reference = clusters;
        } else {
          assertThat(clusters).containsExactlyElementsOf(reference);
        }
      }
    } finally {
      executorService.shutdown();
    }
  }
}
