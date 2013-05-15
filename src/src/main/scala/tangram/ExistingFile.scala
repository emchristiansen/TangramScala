package tangram

import java.io.File

///////////////////////////////////////////////////////////

/**
 * Holds files that exist and are actual files (not directories).
 */
case class ExistingFile(file: File) {
  require(file.isFile, s"${file} is not an existing file.")
}

object ExistingFile {
  def apply(filename: String): ExistingFile = ExistingFile(new File(filename))

  implicit def existingFile2File(self: ExistingFile) =
    self.file
}