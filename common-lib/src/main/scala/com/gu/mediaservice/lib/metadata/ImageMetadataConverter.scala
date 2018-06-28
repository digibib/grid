package com.gu.mediaservice.lib.metadata

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format._

import scala.util.Try

import com.gu.mediaservice.model.{FileMetadata, ImageMetadata}


object ImageMetadataConverter {

  private def extractSubjects(fileMetadata: FileMetadata): List[String] = {
    val supplementalCategories = fileMetadata.iptc
      .get("Supplemental Category(s)")
      .toList.flatMap(_.split("\\s+"))

    val category = fileMetadata.iptc
      .get("Category")

    (supplementalCategories ::: category.toList)
      .flatMap(Subject.create)
      .map(_.toString)
      .distinct
  }

  def fromFileMetadata(fileMetadata: FileMetadata): ImageMetadata =
    ImageMetadata(
      dateTaken           = (fileMetadata.exifSub.get("Date/Time Original Composite") flatMap parseRandomDate) orElse
                            (fileMetadata.iptc.get("Date Time Created Composite") flatMap parseRandomDate) orElse
                            (fileMetadata.xmp.get("photoshop:DateCreated") flatMap parseRandomDate),
      description         = fileMetadata.iptc.get("Caption/Abstract"),
      credit              = fileMetadata.iptc.get("Credit"),
      byline              = fileMetadata.iptc.get("By-line"),
      bylineTitle         = fileMetadata.iptc.get("By-line Title"),
      title               = fileMetadata.iptc.get("Headline"),
      copyrightNotice     = fileMetadata.iptc.get("Copyright Notice"),
      // FIXME: why default to copyrightNotice again?
      copyright           = fileMetadata.exif.get("Copyright") orElse fileMetadata.iptc.get("Copyright Notice"),
      suppliersReference  = fileMetadata.iptc.get("Original Transmission Reference") orElse fileMetadata.iptc.get("Object Name"),
      source              = fileMetadata.iptc.get("Source"),
      specialInstructions = fileMetadata.iptc.get("Special Instructions"),
      keywords            = fileMetadata.iptc.get("Keywords") map (_.split(Array(';', ',')).distinct.map(_.trim).toList) getOrElse Nil,
      subLocation         = fileMetadata.iptc.get("Sub-location"),
      city                = fileMetadata.iptc.get("City"),
      state               = fileMetadata.iptc.get("Province/State"),
      country             = fileMetadata.iptc.get("Country/Primary Location Name"),
      subjects            = extractSubjects(fileMetadata))

  // IPTC doesn't appear to enforce the datetime format of the field, which means we have to
  // optimistically attempt various formats observed in the wild. Dire times.
  private lazy val randomDateFormat = {
    val parsers = Array(
      // 2014-12-16T02:23:45+01:00 - Standard dateTimeNoMillis
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").getParser,
      // 2014-12-16T02:23+01:00 - Same as above but missing seconds lol
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm.SSSZZ").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mmZZ").getParser,
      // Tue Dec 16 01:23:45 GMT 2014 - Let's make machine metadata human readable!
      DateTimeFormat.forPattern("E MMM dd HH:mm:ss.SSS z yyyy").getParser,
      DateTimeFormat.forPattern("E MMM dd HH:mm:ss z yyyy").getParser,
      DateTimeFormat.forPattern("E MMM dd HH:mm:ss.SSS 'BST' yyyy").getParser,
      DateTimeFormat.forPattern("E MMM dd HH:mm:ss 'BST' yyyy").getParser,
      // 2014-12-16 - Maybe it's just a date
      ISODateTimeFormat.date.getParser
    )
    new DateTimeFormatterBuilder().
      append(null, parsers).
      toFormatter
  }

  private def parseRandomDate(str: String): Option[DateTime] =
    safeParsing(randomDateFormat.parseDateTime(str)) map (_.withZone(DateTimeZone.UTC))

  private def safeParsing[A](parse: => A): Option[A] = Try(parse).toOption

}

// TODO: add category
// TODO: add Supplemental Category(s)
//       https://www.iptc.org/std/photometadata/documentation/GenericGuidelines/index.htm#!Documents/guidelineformappingcategorycodestosubjectnewscodes.htm
//       http://www.shutterpoint.com/Help-iptc.cfm#IC
// TODO: add Subject Reference?
//       http://cv.iptc.org/newscodes/subjectcode/
// TODO: add Coded Character Set ?
// TODO: add Application Record Version ?
