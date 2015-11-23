package org.broadinstitute.hellbender.tools.picard.sam;

import htsjdk.samtools.*;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.IOUtil;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.PicardCommandLineProgram;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.ReadProgramGroup;
import org.broadinstitute.hellbender.utils.runtime.ProgressLogger;

import java.io.File;

/**
 * @author alecw@broadinstitute.org
 */
@CommandLineProgramProperties(
        summary = "Sorts the input SAM or BAM.\n" +
                "Input and output formats are determined by file extension.",
        oneLineSummary = "Sorts a SAM or BAM file",
        programGroup = ReadProgramGroup.class
)
public final class SortSam extends PicardCommandLineProgram {

    @Argument(doc = "The BAM or SAM file to sort.",
            fullName = StandardArgumentDefinitions.INPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME)
    public File INPUT;

    @Argument(doc = "The sorted BAM or SAM output file. ",
            fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME)
    public File OUTPUT;

    @Argument(shortName = StandardArgumentDefinitions.SORT_ORDER_SHORT_NAME, doc = "Sort order of output file")
    public SAMFileHeader.SortOrder SORT_ORDER;

    @Override
    protected Object doWork() {
        IOUtil.assertFileIsReadable(INPUT);
        IOUtil.assertFileIsWritable(OUTPUT);
        final SamReader reader = SamReaderFactory.makeDefault().validationStringency(VALIDATION_STRINGENCY).referenceSequence(REFERENCE_SEQUENCE).open(INPUT);
        reader.getFileHeader().setSortOrder(SORT_ORDER);
        try (final SAMFileWriter writer = createSAMWriter(OUTPUT, REFERENCE_SEQUENCE, reader.getFileHeader(), false)) {
            writer.setProgressLogger(
                    new ProgressLogger(logger, (int) 1e7, "Wrote", "records from a sorting collection"));

            final ProgressLogger progress = new ProgressLogger(logger, (int) 1e7, "Read");
            for (final SAMRecord rec : reader) {
                writer.addAlignment(rec);
                progress.record(rec);
            }

            logger.info("Finished reading inputs, merging and writing to output now.");

        }
        CloserUtil.close(reader);
        return null;
    }
}
