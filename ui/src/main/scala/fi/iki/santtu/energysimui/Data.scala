package fi.iki.santtu.energysimui

object Data {
  val finland = """{
                  |    "areas": {
                  |        "central": {
                  |            "drains": [
                  |                {
                  |                    "capacity": 1819,
                  |                    "id": "central-consumption",
                  |                    "type": "finland_consumption"
                  |                }
                  |            ],
                  |            "sources": [
                  |                {
                  |                    "capacity": 538.2,
                  |                    "ghg": 18,
                  |                    "id": "central-bio",
                  |                    "type": "bio"
                  |                },
                  |                {
                  |                    "capacity": 65.0,
                  |                    "ghg": 469,
                  |                    "id": "central-gas",
                  |                    "type": "gas"
                  |                },
                  |                {
                  |                    "capacity": 94.0,
                  |                    "ghg": 470,
                  |                    "id": "central-other",
                  |                    "type": "other"
                  |                },
                  |                {
                  |                    "capacity": 750.9,
                  |                    "ghg": 1145,
                  |                    "id": "central-peat",
                  |                    "type": "peat"
                  |                },
                  |                {
                  |                    "capacity": 577.2,
                  |                    "ghg": 15,
                  |                    "id": "central-wind",
                  |                    "type": "wind"
                  |                },
                  |                {
                  |                    "capacity": 319.2,
                  |                    "ghg": 90,
                  |                    "id": "central-hydro",
                  |                    "type": "hydro"
                  |                },
                  |                {
                  |                    "capacity": 58.2,
                  |                    "ghg": 774,
                  |                    "id": "central-oil",
                  |                    "type": "oil"
                  |                },
                  |                {
                  |                    "capacity": 0,
                  |                    "ghg": 104,
                  |                    "id": "central-solar",
                  |                    "type": "solar"
                  |                }
                  |            ]
                  |        },
                  |        "east": {
                  |            "drains": [
                  |                {
                  |                    "capacity": 1364,
                  |                    "id": "east-consumption",
                  |                    "type": "finland_consumption"
                  |                }
                  |            ],
                  |            "sources": [
                  |                {
                  |                    "capacity": 509.8,
                  |                    "ghg": 18,
                  |                    "id": "east-bio",
                  |                    "type": "bio"
                  |                },
                  |                {
                  |                    "capacity": 9.0,
                  |                    "ghg": 469,
                  |                    "id": "east-gas",
                  |                    "type": "gas"
                  |                },
                  |                {
                  |                    "capacity": 35.4,
                  |                    "ghg": 470,
                  |                    "id": "east-other",
                  |                    "type": "other"
                  |                },
                  |                {
                  |                    "capacity": 263.0,
                  |                    "ghg": 1145,
                  |                    "id": "east-peat",
                  |                    "type": "peat"
                  |                },
                  |                {
                  |                    "capacity": 21.0,
                  |                    "ghg": 15,
                  |                    "id": "east-wind",
                  |                    "type": "wind"
                  |                },
                  |                {
                  |                    "capacity": 375.1,
                  |                    "ghg": 90,
                  |                    "id": "east-hydro",
                  |                    "type": "hydro"
                  |                },
                  |                {
                  |                    "capacity": 282.0,
                  |                    "ghg": 774,
                  |                    "id": "east-oil",
                  |                    "type": "oil"
                  |                },
                  |                {
                  |                    "capacity": 0,
                  |                    "ghg": 104,
                  |                    "id": "east-solar",
                  |                    "type": "solar"
                  |                }
                  |            ]
                  |        },
                  |        "north": {
                  |            "drains": [
                  |                {
                  |                    "capacity": 1324,
                  |                    "id": "north-consumption",
                  |                    "type": "finland_consumption"
                  |                }
                  |            ],
                  |            "sources": [
                  |                {
                  |                    "capacity": 273.8,
                  |                    "ghg": 18,
                  |                    "id": "north-bio"
                  |                },
                  |                {
                  |                    "capacity": 258.4,
                  |                    "ghg": 1145,
                  |                    "id": "north-peat"
                  |                },
                  |                {
                  |                    "capacity": 609.1,
                  |                    "ghg": 15,
                  |                    "id": "north-wind"
                  |                },
                  |                {
                  |                    "capacity": 1700.8,
                  |                    "ghg": 90,
                  |                    "id": "north-hydro"
                  |                },
                  |                {
                  |                    "capacity": 12.9,
                  |                    "ghg": 774,
                  |                    "id": "north-oil"
                  |                },
                  |                {
                  |                    "capacity": 0,
                  |                    "ghg": 104,
                  |                    "id": "north-solar"
                  |                }
                  |            ]
                  |        },
                  |        "south": {
                  |            "drains": [
                  |                {
                  |                    "capacity": 2702,
                  |                    "id": "south-consumption",
                  |                    "type": "finland_consumption"
                  |                }
                  |            ],
                  |            "sources": [
                  |                {
                  |                    "capacity": 485.38,
                  |                    "ghg": 18,
                  |                    "id": "south-bio",
                  |                    "type": "bio"
                  |                },
                  |                {
                  |                    "capacity": 1060.3,
                  |                    "ghg": 1026,
                  |                    "id": "south-coal",
                  |                    "type": "coal"
                  |                },
                  |                {
                  |                    "capacity": 965.6,
                  |                    "ghg": 469,
                  |                    "id": "south-gas",
                  |                    "type": "gas"
                  |                },
                  |                {
                  |                    "capacity": 143.0,
                  |                    "ghg": 470,
                  |                    "id": "south-other",
                  |                    "type": "other"
                  |                },
                  |                {
                  |                    "capacity": 83.54,
                  |                    "ghg": 1145,
                  |                    "id": "south-peat",
                  |                    "type": "peat"
                  |                },
                  |                {
                  |                    "capacity": 56.9,
                  |                    "ghg": 15,
                  |                    "id": "south-wind",
                  |                    "type": "wind"
                  |                },
                  |                {
                  |                    "capacity": 222.6,
                  |                    "ghg": 90,
                  |                    "id": "south-hydro",
                  |                    "type": "hydro"
                  |                },
                  |                {
                  |                    "capacity": 507,
                  |                    "ghg": 40,
                  |                    "id": "loviisa-1",
                  |                    "type": "nuclear"
                  |                },
                  |                {
                  |                    "capacity": 502,
                  |                    "ghg": 40,
                  |                    "id": "loviisa-2",
                  |                    "type": "nuclear"
                  |                },
                  |                {
                  |                    "capacity": 268.6,
                  |                    "ghg": 774,
                  |                    "id": "south-oil",
                  |                    "type": "oil"
                  |                },
                  |                {
                  |                    "capacity": 0,
                  |                    "ghg": 104,
                  |                    "id": "south-solar",
                  |                    "type": "solar"
                  |                }
                  |            ]
                  |        },
                  |        "west": {
                  |            "drains": [
                  |                {
                  |                    "capacity": 2201,
                  |                    "id": "west-consumption",
                  |                    "type": "finland_consumption"
                  |                }
                  |            ],
                  |            "sources": [
                  |                {
                  |                    "capacity": 183.2,
                  |                    "ghg": 18,
                  |                    "id": "west-bio",
                  |                    "type": "bio"
                  |                },
                  |                {
                  |                    "capacity": 1085.0,
                  |                    "ghg": 1026,
                  |                    "id": "west-coal",
                  |                    "type": "coal"
                  |                },
                  |                {
                  |                    "capacity": 375.6,
                  |                    "ghg": 469,
                  |                    "id": "west-gas",
                  |                    "type": "gas"
                  |                },
                  |                {
                  |                    "capacity": 36.2,
                  |                    "ghg": 470,
                  |                    "id": "west-other",
                  |                    "type": "other"
                  |                },
                  |                {
                  |                    "capacity": 342.0,
                  |                    "ghg": 1145,
                  |                    "id": "west-peat",
                  |                    "type": "peat"
                  |                },
                  |                {
                  |                    "capacity": 488.26,
                  |                    "ghg": 15,
                  |                    "id": "west-wind",
                  |                    "type": "wind"
                  |                },
                  |                {
                  |                    "capacity": 407.65,
                  |                    "ghg": 90,
                  |                    "id": "west-hydro",
                  |                    "type": "hydro"
                  |                },
                  |                {
                  |                    "capacity": 880,
                  |                    "ghg": 40,
                  |                    "id": "olkiluoto-1",
                  |                    "type": "nuclear"
                  |                },
                  |                {
                  |                    "capacity": 880,
                  |                    "ghg": 40,
                  |                    "id": "olkiluoto-2",
                  |                    "type": "nuclear"
                  |                },
                  |                {
                  |                    "capacity": 320.2,
                  |                    "ghg": 774,
                  |                    "id": "west-oil",
                  |                    "type": "oil"
                  |                },
                  |                {
                  |                    "capacity": 0,
                  |                    "ghg": 104,
                  |                    "id": "west-solar",
                  |                    "type": "solar"
                  |                }
                  |            ]
                  |        }
                  |    },
                  |    "lines": [
                  |        {
                  |            "areas": [
                  |                "west",
                  |                "south"
                  |            ],
                  |            "capacity": 10000,
                  |            "id": "west-south"
                  |        },
                  |        {
                  |            "areas": [
                  |                "west",
                  |                "central"
                  |            ],
                  |            "capacity": 10000,
                  |            "id": "west-central"
                  |        },
                  |        {
                  |            "areas": [
                  |                "south",
                  |                "east"
                  |            ],
                  |            "capacity": 10000,
                  |            "id": "south-east"
                  |        },
                  |        {
                  |            "areas": [
                  |                "south",
                  |                "central"
                  |            ],
                  |            "capacity": 10000,
                  |            "id": "south-central"
                  |        },
                  |        {
                  |            "areas": [
                  |                "east",
                  |                "central"
                  |            ],
                  |            "capacity": 10000,
                  |            "id": "east-central"
                  |        },
                  |        {
                  |            "areas": [
                  |                "central",
                  |                "north"
                  |            ],
                  |            "capacity": 10000,
                  |            "id": "central-north"
                  |        }
                  |    ],
                  |    "name": "Suomi",
                  |    "types": {
                  |        "bio": {
                  |            "model": "constant"
                  |        },
                  |        "coal": {
                  |            "model": "constant"
                  |        },
                  |        "finland_consumption": {
                  |            "data": {
                  |                "bins": [
                  |                    [
                  |                        6.200909e-05,
                  |                        4000,
                  |                        4500
                  |                    ],
                  |                    [
                  |                        0.0002583712,
                  |                        4500,
                  |                        5000
                  |                    ],
                  |                    [
                  |                        0.001085159,
                  |                        5000,
                  |                        5500
                  |                    ],
                  |                    [
                  |                        0.002759405,
                  |                        5500,
                  |                        6000
                  |                    ],
                  |                    [
                  |                        0.007120711,
                  |                        6000,
                  |                        6500
                  |                    ],
                  |                    [
                  |                        0.02510335,
                  |                        6500,
                  |                        7000
                  |                    ],
                  |                    [
                  |                        0.05067177,
                  |                        7000,
                  |                        7500
                  |                    ],
                  |                    [
                  |                        0.0790926,
                  |                        7500,
                  |                        8000
                  |                    ],
                  |                    [
                  |                        0.0986668,
                  |                        8000,
                  |                        8500
                  |                    ],
                  |                    [
                  |                        0.1244212,
                  |                        8500,
                  |                        9000
                  |                    ],
                  |                    [
                  |                        0.1239975,
                  |                        9000,
                  |                        9500
                  |                    ],
                  |                    [
                  |                        0.1087226,
                  |                        9500,
                  |                        10000
                  |                    ],
                  |                    [
                  |                        0.09949359,
                  |                        10000,
                  |                        10500
                  |                    ],
                  |                    [
                  |                        0.08423936,
                  |                        10500,
                  |                        11000
                  |                    ],
                  |                    [
                  |                        0.06783795,
                  |                        11000,
                  |                        11500
                  |                    ],
                  |                    [
                  |                        0.05353452,
                  |                        11500,
                  |                        12000
                  |                    ],
                  |                    [
                  |                        0.03329888,
                  |                        12000,
                  |                        12500
                  |                    ],
                  |                    [
                  |                        0.01867507,
                  |                        12500,
                  |                        13000
                  |                    ],
                  |                    [
                  |                        0.01155436,
                  |                        13000,
                  |                        13500
                  |                    ],
                  |                    [
                  |                        0.006118231,
                  |                        13500,
                  |                        14000
                  |                    ],
                  |                    [
                  |                        0.002697396,
                  |                        14000,
                  |                        14500
                  |                    ],
                  |                    [
                  |                        0.0005580819,
                  |                        14500,
                  |                        15000
                  |                    ],
                  |                    [
                  |                        3.100455e-05,
                  |                        15000,
                  |                        15500
                  |                    ]
                  |                ],
                  |                "mean": 9575.109
                  |            },
                  |            "model": "scaled"
                  |        },
                  |        "gas": {
                  |            "model": "constant"
                  |        },
                  |        "hydro": {
                  |            "model": "constant"
                  |        },
                  |        "nuclear": {
                  |            "data": [
                  |                [
                  |                    0.02,
                  |                    0,
                  |                    0.9
                  |                ],
                  |                [
                  |                    0.98,
                  |                    1,
                  |                    1
                  |                ]
                  |            ],
                  |            "model": "step"
                  |        },
                  |        "oil": {
                  |            "model": "constant"
                  |        },
                  |        "other": {
                  |            "model": "constant"
                  |        },
                  |        "peat": {
                  |            "model": "constant"
                  |        },
                  |        "solar": {
                  |            "model": "constant"
                  |        },
                  |        "wind": {
                  |            "model": "constant"
                  |        }
                  |    },
                  |    "version": 1
                  |}""".stripMargin
}
