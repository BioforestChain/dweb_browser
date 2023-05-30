import { assertEquals } from "https://deno.land/std@0.187.0/testing/asserts.ts";
import { filePathToUrl } from "../src/utils/file.ts";


/*
* new URL('file:///C:/path/').pathname;      // Incorrect: /C:/path/
* fileURLToPath('file:///C:/path/');         // Correct:   C:\path\ (Windows)
*
* new URL('file://nas/foo.txt').pathname;    // Incorrect: /foo.txt
* fileURLToPath('file://nas/foo.txt');       // Correct:   \\nas\foo.txt (Windows)
*
* new URL('file:///你好.txt').pathname;      // Incorrect: /%E4%BD%A0%E5%A5%BD.txt
* fileURLToPath('file:///你好.txt');         // Correct:   /你好.txt (POSIX)
*
* new URL('file:///hello world').pathname;   // Incorrect: /hello%20world
* fileURLToPath('file:///hello world');      // Correct:   /hello world (POSIX)
*/

Deno.test({
  name:"windows filePathToUrl",
  ignore: Deno.build.os === "linux",
  fn() {
    assertEquals(filePathToUrl("file:///C:/path/"),`C:\\path\\`)
    assertEquals(filePathToUrl("file://nas/foo.txt"),`\\\\nas\\foo.txt`)
  }
})


Deno.test({
  name:"linux filePathToUrl",
  fn() {
    assertEquals(filePathToUrl("file:///你好.txt"),"/你好.txt")
    assertEquals(filePathToUrl("file:///hello world"),"/hello world")
  }
})