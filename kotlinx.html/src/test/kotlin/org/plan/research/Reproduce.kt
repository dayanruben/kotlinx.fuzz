package org.plan.research

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test

object Reproduce {
    @Test
    fun hm() {
        createHTML().html {
            head {
                meta {
                    putButton {
                        b {
                            b {
                                script {
                                    noScript {
                                        putForm {
                                            label {
                                                htmlObject(null) {
                                                    param { }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun ok() {

        createHTML().html {
            head {
                meta {
                    putButton {
                        b {
                            b {
                                script {
                                    noScript {
                                        putForm {
                                            label {
                                                htmlObject {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}