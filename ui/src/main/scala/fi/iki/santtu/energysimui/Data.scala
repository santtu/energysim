package fi.iki.santtu.energysimui

object Data {
//  val finland = """{
//                  |    "areas": {
//                  |        "central": {
//                  |            "drains": [
//                  |                {
//                  |                    "capacity": 1819,
//                  |                    "id": "central-consumption",
//                  |                    "type": "finland_consumption"
//                  |                }
//                  |            ],
//                  |            "sources": [
//                  |                {
//                  |                    "capacity": 538.2,
//                  |                    "ghg": 18,
//                  |                    "id": "central-bio",
//                  |                    "type": "bio"
//                  |                },
//                  |                {
//                  |                    "capacity": 65.0,
//                  |                    "ghg": 469,
//                  |                    "id": "central-gas",
//                  |                    "type": "gas"
//                  |                },
//                  |                {
//                  |                    "capacity": 94.0,
//                  |                    "ghg": 470,
//                  |                    "id": "central-other",
//                  |                    "type": "other"
//                  |                },
//                  |                {
//                  |                    "capacity": 750.9,
//                  |                    "ghg": 1145,
//                  |                    "id": "central-peat",
//                  |                    "type": "peat"
//                  |                },
//                  |                {
//                  |                    "capacity": 577.2,
//                  |                    "ghg": 15,
//                  |                    "id": "central-wind",
//                  |                    "type": "wind"
//                  |                },
//                  |                {
//                  |                    "capacity": 319.2,
//                  |                    "ghg": 90,
//                  |                    "id": "central-hydro",
//                  |                    "type": "hydro"
//                  |                },
//                  |                {
//                  |                    "capacity": 58.2,
//                  |                    "ghg": 774,
//                  |                    "id": "central-oil",
//                  |                    "type": "oil"
//                  |                },
//                  |                {
//                  |                    "capacity": 0,
//                  |                    "ghg": 104,
//                  |                    "id": "central-solar",
//                  |                    "type": "solar"
//                  |                }
//                  |            ]
//                  |        },
//                  |        "east": {
//                  |            "drains": [
//                  |                {
//                  |                    "capacity": 1364,
//                  |                    "id": "east-consumption",
//                  |                    "type": "finland_consumption"
//                  |                }
//                  |            ],
//                  |            "sources": [
//                  |                {
//                  |                    "capacity": 509.8,
//                  |                    "ghg": 18,
//                  |                    "id": "east-bio",
//                  |                    "type": "bio"
//                  |                },
//                  |                {
//                  |                    "capacity": 9.0,
//                  |                    "ghg": 469,
//                  |                    "id": "east-gas",
//                  |                    "type": "gas"
//                  |                },
//                  |                {
//                  |                    "capacity": 35.4,
//                  |                    "ghg": 470,
//                  |                    "id": "east-other",
//                  |                    "type": "other"
//                  |                },
//                  |                {
//                  |                    "capacity": 263.0,
//                  |                    "ghg": 1145,
//                  |                    "id": "east-peat",
//                  |                    "type": "peat"
//                  |                },
//                  |                {
//                  |                    "capacity": 21.0,
//                  |                    "ghg": 15,
//                  |                    "id": "east-wind",
//                  |                    "type": "wind"
//                  |                },
//                  |                {
//                  |                    "capacity": 375.1,
//                  |                    "ghg": 90,
//                  |                    "id": "east-hydro",
//                  |                    "type": "hydro"
//                  |                },
//                  |                {
//                  |                    "capacity": 282.0,
//                  |                    "ghg": 774,
//                  |                    "id": "east-oil",
//                  |                    "type": "oil"
//                  |                },
//                  |                {
//                  |                    "capacity": 0,
//                  |                    "ghg": 104,
//                  |                    "id": "east-solar",
//                  |                    "type": "solar"
//                  |                }
//                  |            ]
//                  |        },
//                  |        "north": {
//                  |            "drains": [
//                  |                {
//                  |                    "capacity": 1324,
//                  |                    "id": "north-consumption",
//                  |                    "type": "finland_consumption"
//                  |                }
//                  |            ],
//                  |            "sources": [
//                  |                {
//                  |                    "capacity": 273.8,
//                  |                    "ghg": 18,
//                  |                    "id": "north-bio"
//                  |                },
//                  |                {
//                  |                    "capacity": 258.4,
//                  |                    "ghg": 1145,
//                  |                    "id": "north-peat"
//                  |                },
//                  |                {
//                  |                    "capacity": 609.1,
//                  |                    "ghg": 15,
//                  |                    "id": "north-wind"
//                  |                },
//                  |                {
//                  |                    "capacity": 1700.8,
//                  |                    "ghg": 90,
//                  |                    "id": "north-hydro"
//                  |                },
//                  |                {
//                  |                    "capacity": 12.9,
//                  |                    "ghg": 774,
//                  |                    "id": "north-oil"
//                  |                },
//                  |                {
//                  |                    "capacity": 0,
//                  |                    "ghg": 104,
//                  |                    "id": "north-solar"
//                  |                }
//                  |            ]
//                  |        },
//                  |        "south": {
//                  |            "drains": [
//                  |                {
//                  |                    "capacity": 2702,
//                  |                    "id": "south-consumption",
//                  |                    "type": "finland_consumption"
//                  |                }
//                  |            ],
//                  |            "sources": [
//                  |                {
//                  |                    "capacity": 485.38,
//                  |                    "ghg": 18,
//                  |                    "id": "south-bio",
//                  |                    "type": "bio"
//                  |                },
//                  |                {
//                  |                    "capacity": 1060.3,
//                  |                    "ghg": 1026,
//                  |                    "id": "south-coal",
//                  |                    "type": "coal"
//                  |                },
//                  |                {
//                  |                    "capacity": 965.6,
//                  |                    "ghg": 469,
//                  |                    "id": "south-gas",
//                  |                    "type": "gas"
//                  |                },
//                  |                {
//                  |                    "capacity": 143.0,
//                  |                    "ghg": 470,
//                  |                    "id": "south-other",
//                  |                    "type": "other"
//                  |                },
//                  |                {
//                  |                    "capacity": 83.54,
//                  |                    "ghg": 1145,
//                  |                    "id": "south-peat",
//                  |                    "type": "peat"
//                  |                },
//                  |                {
//                  |                    "capacity": 56.9,
//                  |                    "ghg": 15,
//                  |                    "id": "south-wind",
//                  |                    "type": "wind"
//                  |                },
//                  |                {
//                  |                    "capacity": 222.6,
//                  |                    "ghg": 90,
//                  |                    "id": "south-hydro",
//                  |                    "type": "hydro"
//                  |                },
//                  |                {
//                  |                    "capacity": 507,
//                  |                    "ghg": 40,
//                  |                    "id": "loviisa-1",
//                  |                    "type": "nuclear"
//                  |                },
//                  |                {
//                  |                    "capacity": 502,
//                  |                    "ghg": 40,
//                  |                    "id": "loviisa-2",
//                  |                    "type": "nuclear"
//                  |                },
//                  |                {
//                  |                    "capacity": 268.6,
//                  |                    "ghg": 774,
//                  |                    "id": "south-oil",
//                  |                    "type": "oil"
//                  |                },
//                  |                {
//                  |                    "capacity": 0,
//                  |                    "ghg": 104,
//                  |                    "id": "south-solar",
//                  |                    "type": "solar"
//                  |                }
//                  |            ]
//                  |        },
//                  |        "west": {
//                  |            "drains": [
//                  |                {
//                  |                    "capacity": 2201,
//                  |                    "id": "west-consumption",
//                  |                    "type": "finland_consumption"
//                  |                }
//                  |            ],
//                  |            "sources": [
//                  |                {
//                  |                    "capacity": 183.2,
//                  |                    "ghg": 18,
//                  |                    "id": "west-bio",
//                  |                    "type": "bio"
//                  |                },
//                  |                {
//                  |                    "capacity": 1085.0,
//                  |                    "ghg": 1026,
//                  |                    "id": "west-coal",
//                  |                    "type": "coal"
//                  |                },
//                  |                {
//                  |                    "capacity": 375.6,
//                  |                    "ghg": 469,
//                  |                    "id": "west-gas",
//                  |                    "type": "gas"
//                  |                },
//                  |                {
//                  |                    "capacity": 36.2,
//                  |                    "ghg": 470,
//                  |                    "id": "west-other",
//                  |                    "type": "other"
//                  |                },
//                  |                {
//                  |                    "capacity": 342.0,
//                  |                    "ghg": 1145,
//                  |                    "id": "west-peat",
//                  |                    "type": "peat"
//                  |                },
//                  |                {
//                  |                    "capacity": 488.26,
//                  |                    "ghg": 15,
//                  |                    "id": "west-wind",
//                  |                    "type": "wind"
//                  |                },
//                  |                {
//                  |                    "capacity": 407.65,
//                  |                    "ghg": 90,
//                  |                    "id": "west-hydro",
//                  |                    "type": "hydro"
//                  |                },
//                  |                {
//                  |                    "capacity": 880,
//                  |                    "ghg": 40,
//                  |                    "id": "olkiluoto-1",
//                  |                    "type": "nuclear"
//                  |                },
//                  |                {
//                  |                    "capacity": 880,
//                  |                    "ghg": 40,
//                  |                    "id": "olkiluoto-2",
//                  |                    "type": "nuclear"
//                  |                },
//                  |                {
//                  |                    "capacity": 320.2,
//                  |                    "ghg": 774,
//                  |                    "id": "west-oil",
//                  |                    "type": "oil"
//                  |                },
//                  |                {
//                  |                    "capacity": 0,
//                  |                    "ghg": 104,
//                  |                    "id": "west-solar",
//                  |                    "type": "solar"
//                  |                }
//                  |            ]
//                  |        }
//                  |    },
//                  |    "lines": [
//                  |        {
//                  |            "areas": [
//                  |                "west",
//                  |                "south"
//                  |            ],
//                  |            "capacity": 10000,
//                  |            "id": "west-south"
//                  |        },
//                  |        {
//                  |            "areas": [
//                  |                "west",
//                  |                "central"
//                  |            ],
//                  |            "capacity": 10000,
//                  |            "id": "west-central"
//                  |        },
//                  |        {
//                  |            "areas": [
//                  |                "south",
//                  |                "east"
//                  |            ],
//                  |            "capacity": 10000,
//                  |            "id": "south-east"
//                  |        },
//                  |        {
//                  |            "areas": [
//                  |                "south",
//                  |                "central"
//                  |            ],
//                  |            "capacity": 10000,
//                  |            "id": "south-central"
//                  |        },
//                  |        {
//                  |            "areas": [
//                  |                "east",
//                  |                "central"
//                  |            ],
//                  |            "capacity": 10000,
//                  |            "id": "east-central"
//                  |        },
//                  |        {
//                  |            "areas": [
//                  |                "central",
//                  |                "north"
//                  |            ],
//                  |            "capacity": 10000,
//                  |            "id": "central-north"
//                  |        }
//                  |    ],
//                  |    "name": "Suomi",
//                  |    "types": {
//                  |        "bio": {
//                  |            "model": "constant"
//                  |        },
//                  |        "coal": {
//                  |            "model": "constant"
//                  |        },
//                  |        "finland_consumption": {
//                  |            "aggregated": true,
//                  |            "data": [
//                  |                [
//                  |                    0.000206697,
//                  |                    0.45,
//                  |                    0.5
//                  |                ],
//                  |                [
//                  |                    0.0006614303,
//                  |                    0.5,
//                  |                    0.55
//                  |                ],
//                  |                [
//                  |                    0.001529558,
//                  |                    0.55,
//                  |                    0.6
//                  |                ],
//                  |                [
//                  |                    0.004350971,
//                  |                    0.6,
//                  |                    0.65
//                  |                ],
//                  |                [
//                  |                    0.01114097,
//                  |                    0.65,
//                  |                    0.7
//                  |                ],
//                  |                [
//                  |                    0.03435304,
//                  |                    0.7,
//                  |                    0.75
//                  |                ],
//                  |                [
//                  |                    0.05672799,
//                  |                    0.75,
//                  |                    0.8
//                  |                ],
//                  |                [
//                  |                    0.08186234,
//                  |                    0.8,
//                  |                    0.85
//                  |                ],
//                  |                [
//                  |                    0.1008681,
//                  |                    0.85,
//                  |                    0.9
//                  |                ],
//                  |                [
//                  |                    0.1223129,
//                  |                    0.9,
//                  |                    0.95
//                  |                ],
//                  |                [
//                  |                    0.115833,
//                  |                    0.95,
//                  |                    1
//                  |                ],
//                  |                [
//                  |                    0.1034725,
//                  |                    1,
//                  |                    1.05
//                  |                ],
//                  |                [
//                  |                    0.09393344,
//                  |                    1.05,
//                  |                    1.1
//                  |                ],
//                  |                [
//                  |                    0.08013642,
//                  |                    1.1,
//                  |                    1.15
//                  |                ],
//                  |                [
//                  |                    0.06501654,
//                  |                    1.15,
//                  |                    1.2
//                  |                ],
//                  |                [
//                  |                    0.05208764,
//                  |                    1.2,
//                  |                    1.25
//                  |                ],
//                  |                [
//                  |                    0.03337123,
//                  |                    1.25,
//                  |                    1.3
//                  |                ],
//                  |                [
//                  |                    0.01880943,
//                  |                    1.3,
//                  |                    1.35
//                  |                ],
//                  |                [
//                  |                    0.01201943,
//                  |                    1.35,
//                  |                    1.4
//                  |                ],
//                  |                [
//                  |                    0.007079372,
//                  |                    1.4,
//                  |                    1.45
//                  |                ],
//                  |                [
//                  |                    0.003203803,
//                  |                    1.45,
//                  |                    1.5
//                  |                ],
//                  |                [
//                  |                    0.0009714758,
//                  |                    1.5,
//                  |                    1.55
//                  |                ],
//                  |                [
//                  |                    5.167425e-05,
//                  |                    1.55,
//                  |                    1.6
//                  |                ]
//                  |            ],
//                  |            "model": "step"
//                  |        },
//                  |        "gas": {
//                  |            "model": "constant"
//                  |        },
//                  |        "hydro": {
//                  |            "model": "constant"
//                  |        },
//                  |        "nuclear": {
//                  |            "aggregated": false,
//                  |            "data": [
//                  |                [
//                  |                    0.02,
//                  |                    0,
//                  |                    0.9
//                  |                ],
//                  |                [
//                  |                    0.98,
//                  |                    1,
//                  |                    1
//                  |                ]
//                  |            ],
//                  |            "model": "step"
//                  |        },
//                  |        "oil": {
//                  |            "model": "constant"
//                  |        },
//                  |        "other": {
//                  |            "model": "constant"
//                  |        },
//                  |        "peat": {
//                  |            "model": "constant"
//                  |        },
//                  |        "solar": {
//                  |            "aggregated": true,
//                  |            "data": [
//                  |                [
//                  |                    0.8976898,
//                  |                    0,
//                  |                    0.02
//                  |                ],
//                  |                [
//                  |                    0.05893446,
//                  |                    0.02,
//                  |                    0.04
//                  |                ],
//                  |                [
//                  |                    0.0155587,
//                  |                    0.04,
//                  |                    0.06
//                  |                ],
//                  |                [
//                  |                    0.009429514,
//                  |                    0.06,
//                  |                    0.08
//                  |                ],
//                  |                [
//                  |                    0.005657709,
//                  |                    0.08,
//                  |                    0.1
//                  |                ],
//                  |                [
//                  |                    0.001414427,
//                  |                    0.1,
//                  |                    0.12
//                  |                ],
//                  |                [
//                  |                    0.005186233,
//                  |                    0.12,
//                  |                    0.14
//                  |                ],
//                  |                [
//                  |                    0.001885903,
//                  |                    0.14,
//                  |                    0.16
//                  |                ],
//                  |                [
//                  |                    0.001885903,
//                  |                    0.16,
//                  |                    0.18
//                  |                ],
//                  |                [
//                  |                    0.0004714757,
//                  |                    0.18,
//                  |                    0.2
//                  |                ],
//                  |                [
//                  |                    0,
//                  |                    0.2,
//                  |                    0.22
//                  |                ],
//                  |                [
//                  |                    0.0004714757,
//                  |                    0.22,
//                  |                    0.24
//                  |                ],
//                  |                [
//                  |                    0,
//                  |                    0.24,
//                  |                    0.26
//                  |                ],
//                  |                [
//                  |                    0.0004714757,
//                  |                    0.26,
//                  |                    0.28
//                  |                ],
//                  |                [
//                  |                    0.0009429514,
//                  |                    0.28,
//                  |                    0.3
//                  |                ]
//                  |            ],
//                  |            "model": "step"
//                  |        },
//                  |        "wind": {
//                  |            "aggregated": true,
//                  |            "data": [
//                  |                [
//                  |                    0.05745769,
//                  |                    0,
//                  |                    0.05
//                  |                ],
//                  |                [
//                  |                    0.1110262,
//                  |                    0.05,
//                  |                    0.1
//                  |                ],
//                  |                [
//                  |                    0.09255272,
//                  |                    0.1,
//                  |                    0.15
//                  |                ],
//                  |                [
//                  |                    0.08502905,
//                  |                    0.15,
//                  |                    0.2
//                  |                ],
//                  |                [
//                  |                    0.08236683,
//                  |                    0.2,
//                  |                    0.25
//                  |                ],
//                  |                [
//                  |                    0.07426442,
//                  |                    0.25,
//                  |                    0.3
//                  |                ],
//                  |                [
//                  |                    0.06479617,
//                  |                    0.3,
//                  |                    0.35
//                  |                ],
//                  |                [
//                  |                    0.05329074,
//                  |                    0.35,
//                  |                    0.4
//                  |                ],
//                  |                [
//                  |                    0.04363729,
//                  |                    0.4,
//                  |                    0.45
//                  |                ],
//                  |                [
//                  |                    0.05505012,
//                  |                    0.45,
//                  |                    0.5
//                  |                ],
//                  |                [
//                  |                    0.04791999,
//                  |                    0.5,
//                  |                    0.55
//                  |                ],
//                  |                [
//                  |                    0.05472602,
//                  |                    0.55,
//                  |                    0.6
//                  |                ],
//                  |                [
//                  |                    0.06331458,
//                  |                    0.6,
//                  |                    0.65
//                  |                ],
//                  |                [
//                  |                    0.06562956,
//                  |                    0.65,
//                  |                    0.7
//                  |                ],
//                  |                [
//                  |                    0.04384564,
//                  |                    0.7,
//                  |                    0.75
//                  |                ],
//                  |                [
//                  |                    0.005092946,
//                  |                    0.75,
//                  |                    0.8
//                  |                ]
//                  |            ],
//                  |            "model": "step"
//                  |        }
//                  |    },
//                  |    "version": 1
//                  |}""".stripMargin
//
//  val map =
//    """<svg width="100%" height="100%" viewBox="0 0 1634 2904" preserveAspectRatio="xMidYMid meet" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" xml:space="preserve" xmlns:serif="http://www.serif.com/" style="fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:1.41421;">
//      |<path id="north" d="M593.177,1450.1l40.456,-243.345l-179.026,-109.669l0,-277.058l-445.607,-302.959l87.731,-143.829l253.392,195.777l218.473,0l0,-485.164l179.026,-74.854l190.104,74.854l0,394.419l185.809,272.591l0,272.401l169.413,296.046l29.913,238.463l-729.684,-107.673Z" style="fill:#006919;"/><path id="east" d="M1084.78,1551.41l245.514,31.6l293.981,294.592l-293.981,601.027l-172.3,-167.326l-150.468,-431.952l77.254,-327.941Z" style="fill:#006919;"/><path id="center" d="M361.714,1877.6l221.318,-402.975l475.245,72.293l-78.74,330.682l152.217,443.563l-439.823,154.05l-108.899,-416.314l-221.318,-181.299Z" style="fill:#006919;"/><g id="west"><path id="land" d="M208.022,2131.96l153.692,292.695l-64.697,236.47l242.877,233.33l88.128,-94.144l32.666,-313.896l-98.422,-409.414l-224.134,-179.298l-130.11,234.257Z" style="fill:#006919;"/><path id="aland" d="M137.698,2747.76l0,146.695l140.647,0l-140.647,-146.695Z" style="fill:#006919;"/></g><path id="south" d="M654.603,2810.13l36.381,-306.905l455.506,-156.486l167.018,164.659l-410.996,236.361l-247.909,62.371Z" style="fill:#006919;"/>
//      |  <defs>
//      |    <filter id="shadow" x="0" y="0" width="200%" height="200%">
//      |      <feOffset result="offOut" in="SourceAlpha" dx="20" dy="20" />
//      |      <feGaussianBlur result="blurOut" in="offOut" stdDeviation="10" />
//      |      <feBlend in="SourceGraphic" in2="blurOut" mode="normal" />
//      |    </filter>
//      |  </defs>
//      |   <animate id="northfocus"
//      |       attributeName="viewBox" begin="indefinite"
//      |       dur="0.5s" to="5 5 1320 1550"
//      |       fill="freeze"/>
//      |   <animate id="worldfocus"
//      |   attributeName="viewBox" begin="indefinite"
//      |   dur="0.3s" to="0 0 1634 2904" fill="freeze" />
//      |  </svg>""".stripMargin


  final val worldUrl = "/data/world.json"
  final val mapUrl = "/data/map.xml"
}

