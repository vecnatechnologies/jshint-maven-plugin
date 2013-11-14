// definedGlobal & definedGlobal2 are activated in jshintrc and the pom; undefinedglobal is undefined
if (definedGlobal.z && definedGlobal2.z) {
  undefinedglobal.z = "zzz"
}