package boogieloops.kit

import utest._

object DotEnvTests extends TestSuite {
  val tests = Tests {
    test("load env file") {
      val wd = os.temp.dir()
      val envFile = wd / ".env"
      os.write(envFile, "TEST_KEY=test_value\n#Comment\nQUOTED=\"quoted value\"")

      val dotEnv = DotEnv.load(envFile.toString)

      assert(dotEnv.get("TEST_KEY").contains("test_value"))
      assert(dotEnv.get("QUOTED").contains("quoted value"))
      assert(dotEnv.get("NON_EXISTENT").isEmpty)
    }

    test("fallback to sys.env") {
      val dotEnv = new DotEnv(Map.empty)
      val pathEnv = dotEnv.get("PATH")
      assert(pathEnv.isDefined)
    }

    test("fallback to default") {
      val dotEnv = new DotEnv(Map.empty)
      assert(dotEnv.getOrElse("MISSING_KEY", "default") == "default")
    }

    test("withSet returns new instance") {
      val dotEnv1 = new DotEnv(Map.empty)
      val dotEnv2 = dotEnv1.withSet("NEW_KEY", "new_value")

      assert(dotEnv1.get("NEW_KEY").isEmpty)
      assert(dotEnv2.get("NEW_KEY").contains("new_value"))
    }

    test("all returns unified view") {
      val wd = os.temp.dir()
      val envFile = wd / ".env"
      os.write(envFile, "APP_NAME=MyApp")

      val dotEnv = DotEnv.load(envFile.toString)
      val allVars = dotEnv.all

      assert(allVars.contains("APP_NAME"))
      assert(allVars.contains("PATH"))
      assert(allVars("APP_NAME") == "MyApp")
    }

    test("file values override sys.env in all") {
      val wd = os.temp.dir()
      val envFile = wd / ".env"
      os.write(envFile, "PATH=/custom/path")

      val dotEnv = DotEnv.load(envFile.toString)
      val allVars = dotEnv.all

      assert(allVars("PATH") == "/custom/path")
    }

    test("handling missing file") {
      val dotEnv = DotEnv.load("non_existent_file")
      assert(dotEnv.all.nonEmpty)
    }

    test("quote handling edge cases") {
      val wd = os.temp.dir()
      val envFile = wd / ".env"
      os.write(envFile, "SINGLE='a'\nEMPTY=\"\"\nNO_QUOTE=value")

      val dotEnv = DotEnv.load(envFile.toString)

      assert(dotEnv.get("SINGLE").contains("a"))
      assert(dotEnv.get("EMPTY").contains(""))
      assert(dotEnv.get("NO_QUOTE").contains("value"))
    }

    test("overrideExisting false uses first occurrence") {
      val wd = os.temp.dir()
      val envFile = wd / ".env"
      os.write(envFile, "KEY=first\nKEY=second")

      val dotEnv = DotEnv.load(envFile.toString, overrideExisting = false)

      assert(dotEnv.get("KEY").contains("first"))
    }

    test("overrideExisting true uses last occurrence") {
      val wd = os.temp.dir()
      val envFile = wd / ".env"
      os.write(envFile, "KEY=first\nKEY=second")

      val dotEnv = DotEnv.load(envFile.toString, overrideExisting = true)

      assert(dotEnv.get("KEY").contains("second"))
    }

    test("equals sign in value") {
      val wd = os.temp.dir()
      val envFile = wd / ".env"
      os.write(envFile, "CONNECTION=host=localhost;port=5432")

      val dotEnv = DotEnv.load(envFile.toString)

      assert(dotEnv.get("CONNECTION").contains("host=localhost;port=5432"))
    }
  }
}
