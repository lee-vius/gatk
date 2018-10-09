package org.broadinstitute.hellbender.tools.funcotator;

import com.google.common.collect.Lists;
import htsjdk.samtools.util.Locatable;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.testutils.ArgumentsBuilder;
import org.broadinstitute.hellbender.tools.copynumber.arguments.CopyNumberStandardArgument;
import org.broadinstitute.hellbender.tools.copynumber.utils.annotatedinterval.AnnotatedInterval;
import org.broadinstitute.hellbender.tools.copynumber.utils.annotatedinterval.AnnotatedIntervalCollection;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.io.Resource;
import org.broadinstitute.hellbender.utils.test.FuncotatorTestUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FuncotateSegmentsIntegrationTest extends CommandLineProgramTest {
    private static final String TEST_SUB_DIR = toolsTestDir + "/funcotator/";
    private static final String SIMPLE_TEST_FILE = TEST_SUB_DIR + "simple.seg";
    private static final String SIMPLE_TEST_CNTN4_FILE = TEST_SUB_DIR + "simple_cntn4_overlap.seg";
    private static final String TEST_GATK_FILE_B37 = TEST_SUB_DIR + "SM-74NF5.called.seg";
    private static final String TEST_GATK_EMPTY_FILE_B37 = TEST_SUB_DIR + "empty_b37.seg";
    private static final String REF = b37Reference;
    private static final String DS_PIK3CA_DIR  = largeFileTestDir + "funcotator" + File.separator + "small_ds_pik3ca" + File.separator;
    // This has transcripts with multiple gene names...
    private static final String DS_CNTN4_DIR  = toolsTestDir + "funcotator" + File.separator + "small_cntn4_ds" + File.separator;
    private static final String SEG_RESOURCE_FILE = "org/broadinstitute/hellbender/tools/funcotator/simple_funcotator_seg_file.config";
    private static final String GENE_LIST_RESOURCE_FILE = "org/broadinstitute/hellbender/tools/funcotator/gene_list_output.config";

    // TODO: Add Gene list checks.

    @Test
    public void testSimpleNoOverlap() throws IOException {
        final File outputFile = File.createTempFile("funcotatesegs_simple", ".seg");

        final ArgumentsBuilder arguments = new ArgumentsBuilder();
        arguments.add("--" + CopyNumberStandardArgument.SEGMENTS_FILE_LONG_NAME);
        arguments.add(SIMPLE_TEST_FILE);
        arguments.add("--" + FuncotatorArgumentDefinitions.OUTPUT_FORMAT_LONG_NAME);
        arguments.add(FuncotatorArgumentDefinitions.OutputFormatType.SEG);
        arguments.add("--" + StandardArgumentDefinitions.REFERENCE_LONG_NAME);
        arguments.add(REF);
        arguments.add("-" + StandardArgumentDefinitions.OUTPUT_SHORT_NAME);
        arguments.add(outputFile.getAbsolutePath());
        arguments.add("--" + FuncotatorArgumentDefinitions.REFERENCE_VERSION_LONG_NAME);
        arguments.add("hg19");
        arguments.addArgument(FuncotatorArgumentDefinitions.DATA_SOURCES_PATH_LONG_NAME, DS_PIK3CA_DIR);

        runCommandLine(arguments);

        final AnnotatedIntervalCollection collection = AnnotatedIntervalCollection.create(outputFile.toPath(), null);
        Assert.assertEquals(collection.getRecords().size(), 3);
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.hasAnnotation("genes")));
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.getAnnotationValue("genes").equals("")));
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.getAnnotationValue("start_gene").equals("")));
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.getAnnotationValue("end_gene").equals("")));
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.getAnnotationValue("start_exon").equals("")));
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.getAnnotationValue("end_exon").equals("")));
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.getAnnotationValue("ref_allele").equals("")));
        Assert.assertTrue(collection.getRecords().stream().allMatch(r -> r.getAnnotationValue("alt_allele").equals("")));


    }

    /**
     * Very dependent on the data in "simple_cntn4_overlap.seg"
     */
    @DataProvider
    public Object[][] cntn4GroundTruth() {
        return new Object[][] {
                {
                            // Locatable info
                            Arrays.asList(new SimpleInterval("3", 2000000, 2500000),
                                    new SimpleInterval("3", 3000000,3500000),
                                    new SimpleInterval("3",3500001,3900000)),

                            // genes
                            Arrays.asList("CNTN4,CNTN4-AS2", "CNTN4,CNTN4-AS1", ""),
                            //start_gene
                            Arrays.asList("", "CNTN4", ""),
                            //end_gene
                            Arrays.asList("CNTN4", "", ""),
                            // ref_allele (always blank)
                            Arrays.asList("", "", ""),
                            // alt_allele (always blank)
                            Arrays.asList("", "", ""),
                            // Call
                            Arrays.asList("0", "-", "+"),
                            // Segment_Mean
                            Arrays.asList("0.037099", "0.001748", "0.501748"),
                            // Num_Probes
                            Arrays.asList("2000", "3000", "4000"),
                            // Sample
                            Arrays.asList("SAMPLE1", "SAMPLE1", "SAMPLE1")
                }
        };
    }

    @Test(dataProvider = "cntn4GroundTruth")
    public void testSimpleMultipleGenesOverlap(List<Locatable> gtInterval, List<String> gtGenesValues, List<String> gtStartGeneValues, List<String> gtEndGeneValues,
                                               List<String> gtRefAlleles, List<String> gtAltAlleles, List<String> gtCalls,
                                               List<String> gtSegmentMeans, List<String> gtNumProbes, List<String> gtSamples)
            throws IOException {
        final File outputFile = File.createTempFile("funcotatesegs_simple_cntn4", ".seg");

        final ArgumentsBuilder arguments = new ArgumentsBuilder();
        arguments.add("--" + CopyNumberStandardArgument.SEGMENTS_FILE_LONG_NAME);
        arguments.add(SIMPLE_TEST_CNTN4_FILE);
        arguments.add("--" + FuncotatorArgumentDefinitions.OUTPUT_FORMAT_LONG_NAME);
        arguments.add(FuncotatorArgumentDefinitions.OutputFormatType.SEG);
        arguments.add("--" + StandardArgumentDefinitions.REFERENCE_LONG_NAME);
        arguments.add(REF);
        arguments.add("-" + StandardArgumentDefinitions.OUTPUT_SHORT_NAME);
        arguments.add(outputFile.getAbsolutePath());
        arguments.add("--" + FuncotatorArgumentDefinitions.REFERENCE_VERSION_LONG_NAME);
        arguments.add("hg19");
        arguments.addArgument(FuncotatorArgumentDefinitions.DATA_SOURCES_PATH_LONG_NAME, DS_CNTN4_DIR);

        runCommandLine(arguments);

        final AnnotatedIntervalCollection collection = AnnotatedIntervalCollection.create(outputFile.toPath(), null);
        Assert.assertEquals(collection.getRecords().size(), 3);

        final List<String> testGenesValues = collection.getRecords().stream().map(r -> r.getAnnotationValue("genes")).collect(Collectors.toList());
        Assert.assertEquals(testGenesValues, gtGenesValues);
        final List<String> testStartGeneValues = collection.getRecords().stream().map(r -> r.getAnnotationValue("start_gene")).collect(Collectors.toList());
        Assert.assertEquals(testStartGeneValues, gtStartGeneValues);
        final List<String> testEndGeneValues = collection.getRecords().stream().map(r -> r.getAnnotationValue("end_gene")).collect(Collectors.toList());
        Assert.assertEquals(testEndGeneValues, gtEndGeneValues);
        final List<String> testRefAlleleValues = collection.getRecords().stream().map(r -> r.getAnnotationValue("ref_allele")).collect(Collectors.toList());
        Assert.assertEquals(testRefAlleleValues, gtRefAlleles);
        final List<String> testAltAlleleValues = collection.getRecords().stream().map(r -> r.getAnnotationValue("alt_allele")).collect(Collectors.toList());
        Assert.assertEquals(testAltAlleleValues, gtAltAlleles);

        Assert.assertEquals(collection.getRecords().stream().map(r -> r.getAnnotationValue("Num_Probes")).collect(Collectors.toList()), gtNumProbes);
        Assert.assertEquals(collection.getRecords().stream().map(r -> r.getAnnotationValue("Segment_Mean")).collect(Collectors.toList()), gtSegmentMeans);
        Assert.assertEquals(collection.getRecords().stream().map(r -> r.getAnnotationValue("Segment_Call")).collect(Collectors.toList()), gtCalls);
        Assert.assertEquals(collection.getRecords().stream().map(r -> r.getAnnotationValue("Sample")).collect(Collectors.toList()), gtSamples);
        Assert.assertEquals(collection.getRecords().stream().map(r -> r.getInterval()).collect(Collectors.toList()), gtInterval);
    }

    @Test
    public void testGatkCalledSegmentFile() throws IOException {
        final File outputFile = File.createTempFile("funcotatesegs_gatk_called", ".seg");

        final ArgumentsBuilder arguments = new ArgumentsBuilder();
        arguments.add("--" + CopyNumberStandardArgument.SEGMENTS_FILE_LONG_NAME);
        arguments.add(TEST_GATK_FILE_B37);
        arguments.add("--" + FuncotatorArgumentDefinitions.OUTPUT_FORMAT_LONG_NAME);
        arguments.add(FuncotatorArgumentDefinitions.OutputFormatType.SEG);
        arguments.add("--" + StandardArgumentDefinitions.REFERENCE_LONG_NAME);
        arguments.add(REF);
        arguments.add("-" + StandardArgumentDefinitions.OUTPUT_SHORT_NAME);
        arguments.add(outputFile.getAbsolutePath());
        arguments.add("--" + FuncotatorArgumentDefinitions.REFERENCE_VERSION_LONG_NAME);
        arguments.add("hg19");
        arguments.addArgument(FuncotatorArgumentDefinitions.DATA_SOURCES_PATH_LONG_NAME, DS_CNTN4_DIR);

        runCommandLine(arguments);

        final AnnotatedIntervalCollection outputSegmentCollection = AnnotatedIntervalCollection.create(outputFile.toPath(), null);
        Assert.assertEquals(outputSegmentCollection.getRecords().size(), 404);

        // This tests that all should-be-populated fields have a value
        final List<String> allSegmentFieldsThatShouldBePopulated = Arrays.asList("Num_Probes", "Segment_Mean", "Segment_Call");
        final List<String> allSegmentFieldsThatShouldBeEmpty = Arrays.asList("ref_allele", "alt_allele");
        allSegmentFieldsThatShouldBePopulated.forEach(f ->
            Assert.assertTrue(outputSegmentCollection.getRecords().stream().noneMatch(r -> StringUtils.isEmpty(r.getAnnotationValue(f))), f + " was not populated and it should be.")
        );
        allSegmentFieldsThatShouldBeEmpty.forEach(f ->
            Assert.assertTrue(outputSegmentCollection.getRecords().stream().allMatch(r -> StringUtils.isEmpty(r.getAnnotationValue(f))), f + " was populated and it should not be.")
        );

        // Left is the output name.  Right is the input name.  These fields should have the exact same values in the input and output
        final List<Pair<String,String>> testPairedFieldNames = Arrays.asList(
          Pair.of("Num_Probes", "NUM_POINTS_COPY_RATIO"),
                Pair.of("Segment_Mean", "MEAN_LOG2_COPY_RATIO"),
                Pair.of("Segment_Call", "CALL"),
                Pair.of("chr", "CONTIG"),
                Pair.of("start", "START"),
                Pair.of("end", "END")
        );

        for (final Pair<String,String> pairedFields : testPairedFieldNames) {
            final AnnotatedIntervalCollection inputSegmentCollection = AnnotatedIntervalCollection.create(Paths.get(TEST_GATK_FILE_B37), null);
            Assert.assertEquals(outputSegmentCollection.getRecords().stream().map(r -> r.getAnnotationValue(pairedFields.getLeft())).collect(Collectors.toList()),
                    inputSegmentCollection.getRecords().stream().map(r -> r.getAnnotationValue(pairedFields.getRight())).collect(Collectors.toList()));
        }
        // We should have one segment that has all CNTN4 genes
        final AnnotatedInterval cntn4Segment = outputSegmentCollection.getRecords().stream().filter(r -> !StringUtils.isEmpty(r.getAnnotationValue("genes"))).findFirst().get();
        Assert.assertEquals(cntn4Segment.getAnnotationValue("genes"), "CNTN4,CNTN4-AS1,CNTN4-AS2");
    }

    // TODO: hg38 test

    @Test
    public void testEmptyGatkCalledSegmentFile() throws IOException {
        final File outputFile = File.createTempFile("funcotatesegs_gatk_called", ".seg");

        final ArgumentsBuilder arguments = new ArgumentsBuilder();
        arguments.add("--" + CopyNumberStandardArgument.SEGMENTS_FILE_LONG_NAME);
        arguments.add(TEST_GATK_EMPTY_FILE_B37);
        arguments.add("--" + FuncotatorArgumentDefinitions.OUTPUT_FORMAT_LONG_NAME);
        arguments.add(FuncotatorArgumentDefinitions.OutputFormatType.SEG);
        arguments.add("--" + StandardArgumentDefinitions.REFERENCE_LONG_NAME);
        arguments.add(REF);
        arguments.add("-" + StandardArgumentDefinitions.OUTPUT_SHORT_NAME);
        arguments.add(outputFile.getAbsolutePath());
        arguments.add("--" + FuncotatorArgumentDefinitions.REFERENCE_VERSION_LONG_NAME);
        arguments.add("hg19");
        arguments.addArgument(FuncotatorArgumentDefinitions.DATA_SOURCES_PATH_LONG_NAME, DS_CNTN4_DIR);

        runCommandLine(arguments);

        // Just read the resource config file for segments to get the column list
        final Path configFile = Resource.getResourceContentsAsFile(SEG_RESOURCE_FILE).toPath();
        try {
            final Configuration configFileContents = new Configurations().properties(configFile.toFile());
            final List<String> expectedColumns = Lists.newArrayList(configFileContents.getKeys());
            final List<String> guessColumns = FuncotatorTestUtils.createLinkedHashMapListTableReader(outputFile).columns().names();
            Assert.assertEquals(guessColumns, expectedColumns);
        } catch (final ConfigurationException ce) {
            throw new UserException.BadInput("Unable to read from XSV config file: " + configFile.toUri().toString(), ce);
        }
    }
}